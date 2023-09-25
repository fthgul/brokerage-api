package com.midas.studycase.brokerageapi.service.cache;

import com.midas.studycase.brokerageapi.model.enums.OrderStatus;
import com.midas.studycase.brokerageapi.model.enums.OrderType;
import com.midas.studycase.brokerageapi.model.event.OrderEvent;
import com.midas.studycase.brokerageapi.model.response.OrderDetailResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RMapReactive;
import org.redisson.api.RScoredSortedSetReactive;
import org.redisson.api.RedissonReactiveClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

/**
 * Service class for caching OrderEvent objects and retrieving them from Redis.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderRedisReactiveService extends BaseOrderService {

    private final RedissonReactiveClient redissonReactiveClient;

    /**
     * Caches the order details in Redis based on the given OrderEvent and order status.
     * If there is an existing hash structure for the OrderID, it updates the currentStatus and history fields.
     * Otherwise, it creates a new hash structure.
     * Additionally, it sets the order in the user's SortedSet of orders with the order timestamp.
     *
     * @param orderEvent  The OrderEvent containing order details.
     * @param orderStatus The status of the order.
     * @return A Mono indicating the completion of the caching process.
     */
    public Mono<Boolean> cacheOrder(OrderEvent orderEvent, OrderStatus orderStatus) {
        String orderKey = generateOrderKey(orderEvent.getOrderId());
        RMapReactive<String, Object> orderMap = redissonReactiveClient.getMap(orderKey);
        RScoredSortedSetReactive<String> userOrdersSet = redissonReactiveClient.getScoredSortedSet("user:" + orderEvent.getUserId() + ":orders");

        return orderMap.isExists()
                .flatMap(exists -> exists ? updateOrderHistory(orderMap, orderEvent) : createNewOrder(orderMap, orderEvent, orderStatus))
                .then(userOrdersSet.add(LocalDateTime.now().toEpochSecond(ZoneOffset.UTC), orderEvent.getOrderId()))
                .then(Mono.just(true));
    }

    /**
     * Retrieves the last  orders of a user from Redis and returns their details as a list of OrderDetailResponse.
     *
     * @param userId The ID of the user whose orders are to be fetched.
     * @return A Flux emitting the details of the last 10 orders of the user as a list of OrderDetailResponse.
     */
    public Flux<OrderDetailResponse> getLastOrdersForUser(Long userId, Integer page, Integer size) {
        return fetchLast10OrderIdsForUser(userId, page, size)
                .flatMap(this::fetchOrderDetailsFromRedis)
                .collectList()
                .flatMapMany(this::mergeOrderHistories);
    }


    /**
     * Removes the cached order details from Redis based on the given orderId.
     * Additionally, it removes the order from the user's SortedSet of orders.
     *
     * @param orderId The ID of the order to be removed.
     * @param userId  The ID of the user associated with the order.
     * @return A Mono indicating the completion of the removal process.
     */
    public Mono<Boolean> removeCachedOrder(String orderId, Long userId) {
        String orderKey = generateOrderKey(orderId);
        RMapReactive<String, Object> orderMap = redissonReactiveClient.getMap(orderKey);
        RScoredSortedSetReactive<String> userOrdersSet = redissonReactiveClient.getScoredSortedSet("user:" + userId + ":orders");

        return orderMap.delete() // Remove the order details from the cache
                .then(userOrdersSet.remove(orderId)) // Remove the order from the user's SortedSet of orders
                .then(Mono.just(true));
    }



    /**
     * Retrieves an OrderDetailResponse object from Redis cache using orderId.
     *
     * @param orderId The ID of the order.
     * @return A Mono<OrderDetailResponse> containing the cached OrderDetailResponse object, if found.
     */
    public Mono<OrderDetailResponse> getOrderFromCache(String orderId) {
        RMapReactive<String, Object> orderDetailsMap = redissonReactiveClient.getMap(getOrderKey(orderId));

        return orderDetailsMap.readAllMap()
                .filter(cachedOrderMap -> !cachedOrderMap.isEmpty())
                .map(this::convertMapToOrderDetailResponse)
                .doOnSuccess(cacheOrder -> log.info("Successfully retrieved from cache for key: {}", orderId))
                .doOnError(error -> log.error("Failed to retrieve from cache for key: {}", orderId))
                .switchIfEmpty(Mono.empty());
    }


    private OrderDetailResponse convertMapToOrderDetailResponse(Map<String, Object> cachedOrderMap) {
        OrderDetailResponse response = new OrderDetailResponse();
        response.setOrderId(getStringValue(cachedOrderMap, "orderId"));
        response.setUserId(getStringValue(cachedOrderMap,"userId"));
        response.setStatus(getEnumValue(cachedOrderMap,"currentStatus", OrderStatus.class));
        response.setTicker(getStringValue(cachedOrderMap, "ticker"));
        response.setQuantity(getIntValue(cachedOrderMap,"quantity"));
        response.setCreatedAt(getDateValue(cachedOrderMap,"createdAt"));
        response.setUpdatedAt(getDateValue(cachedOrderMap,"updatedAt"));

        // History parsing logic
        List<OrderDetailResponse.OrderHistory> orderHistoryList = new ArrayList<>();
        List<Map<String, String>> historyMapList = (List<Map<String, String>>) cachedOrderMap.get("history");
        for (Map<String, String> historyMap : historyMapList) {
            OrderDetailResponse.OrderHistory orderHistory = new OrderDetailResponse.OrderHistory();
            orderHistory.setOrderType(OrderType.valueOf(historyMap.get("orderType")));
            orderHistory.setCreatedAt(LocalDateTime.parse(historyMap.get("timestamp"), FORMATTER));
            orderHistoryList.add(orderHistory);
        }

        response.setOrderHistories(orderHistoryList);

        return response;
    }

    private List<OrderDetailResponse.OrderHistory> parseHistory(String historyString) {
        if(!StringUtils.hasText(historyString)) {
            return Collections.emptyList();
        }
        List<OrderDetailResponse.OrderHistory> orderHistories = new ArrayList<>();
        String[] historyEntries = historyString.split(",");
        for (String entry : historyEntries) {
            String[] parts = entry.split("@");
            if (parts.length == 2) {
                OrderDetailResponse.OrderHistory orderHistory = new OrderDetailResponse.OrderHistory();
                orderHistory.setOrderType(OrderType.valueOf(parts[0]));
                orderHistory.setCreatedAt(LocalDateTime.parse(parts[1]));
                orderHistories.add(orderHistory);
            }
        }
        return orderHistories;
    }


    /**
     * Updates the order history in the given order map with the provided order event.
     *
     * @param orderMap The reactive map containing order details.
     * @param orderEvent The event containing order information to be added to the history.
     * @return A Mono<Void> indicating completion of the update operation.
     * @throws IllegalArgumentException if the history in the order map is not of the expected type List<Map<String, String>>.
     */
    private Mono<Void> updateOrderHistory(RMapReactive<String, Object> orderMap, OrderEvent orderEvent) {
        return orderMap.get("history")
                .defaultIfEmpty(new ArrayList<Map<String, String>>())
                .flatMap(history -> {
                    if (!(history instanceof List)) {
                        return Mono.error(new IllegalArgumentException("History is not of expected type List<Map<String, String>>"));
                    }
                    List<Map<String, String>> orderHistoryList = (List<Map<String, String>>) history;
                    Map<String, String> newHistoryEntry = createNewHistoryEntry(orderEvent);
                    orderHistoryList.add(newHistoryEntry);
                    return orderMap.put("history", orderHistoryList).then();
                });
    }

    /**
     * Creates a new history entry map from the given order event.
     *
     * @param orderEvent The event containing order information.
     * @return A map representing the new history entry.
     */
    private Map<String, String> createNewHistoryEntry(OrderEvent orderEvent) {
        Map<String, String> newHistoryEntry = new HashMap<>();
        newHistoryEntry.put("orderType", orderEvent.getOrderType().toString());
        newHistoryEntry.put("timestamp", LocalDateTime.now().toString());
        return newHistoryEntry;
    }



    private Mono<Void> createNewOrder(RMapReactive<String, Object> orderMap, OrderEvent orderEvent, OrderStatus orderStatus) {
        Map<String, Object> orderData = createOrderMap(orderEvent, orderStatus);
        return orderMap.putAll(orderData);
    }

    private Flux<String> fetchLast10OrderIdsForUser(Long userId, Integer page, Integer size) {
        RScoredSortedSetReactive<String> userOrdersSet = redissonReactiveClient.getScoredSortedSet("user:" + userId + ":orders");
        return userOrdersSet.valueRangeReversed(page, size)
                .flatMapMany(Flux::fromIterable);
    }

    private Flux<Map<String, Object>> fetchOrderDetailsFromRedis(String orderId) {
        RMapReactive<String, Object> orderMap = redissonReactiveClient.getMap(generateOrderKey(orderId));
        return orderMap.readAllMap().flux();
    }

    private Flux<OrderDetailResponse> mergeOrderHistories(List<Map<String, Object>> cachedOrders) {
        Map<String, OrderDetailResponse> orderResponseMap = new HashMap<>();

        for (Map<String, Object> cachedOrderMap : cachedOrders) {
            String orderId = getStringValue(cachedOrderMap, "orderId");
            OrderDetailResponse existingResponse = orderResponseMap.get(orderId);

            if (existingResponse == null) {
                existingResponse = convertMapToOrderDetailResponse(cachedOrderMap);
                orderResponseMap.put(orderId, existingResponse);
            } else {
                String historyString = (String) cachedOrderMap.get("history");
                List<OrderDetailResponse.OrderHistory> orderHistories = parseHistory(historyString);
                existingResponse.getOrderHistories().addAll(orderHistories);
            }
        }

        return Flux.fromIterable(orderResponseMap.values());
    }


}
