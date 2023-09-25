package com.midas.studycase.brokerageapi.service.cache;

import com.midas.studycase.brokerageapi.model.enums.OrderStatus;
import com.midas.studycase.brokerageapi.model.enums.OrderType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RKeys;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderRedisService extends BaseOrderService {

    private final RedissonClient redissonClient;

    /**
     * Checks if there is any OrderEvent with orderType as CANCEL associated with the given orderId in Redis cache.
     *
     * @param orderId The ID of the order.
     * @return A boolean value indicating whether there is a CANCEL OrderEvent.
     */
    public boolean isCancelledOrderInCache(String orderId) {
        String orderKey = generateOrderKey(orderId);
        RMap<String, Object> orderMap = redissonClient.getMap(orderKey);

        if (orderMap.isExists()) {
            final OrderStatus currentStatus = getEnumValue(orderMap, "currentStatus", OrderStatus.class);
            if(OrderStatus.CREATED == currentStatus) {
                Object historyObj = orderMap.get("history");
                if (historyObj instanceof List) {
                    List<Map<String, String>> historyMapList = (List<Map<String, String>>) historyObj;
                    return historyMapList.stream()
                            .anyMatch(historyMap -> OrderType.CANCEL.name().equals(historyMap.get("orderType")));
                }
            }

            return OrderStatus.CANCELLED == currentStatus;

        }

        return false; // No cancelled order found
    }



    /**
     * Updates the status and updatedAt fields of an OrderEvent associated with the given orderId in Redis cache.
     *
     * @param orderId     The ID of the order.
     * @param orderStatus The new status to be set for the order.
     */
    public void updateOrderStatusInCache(String orderId, OrderStatus orderStatus) {
        String orderKey = generateOrderKey(orderId);
        RMap<String, Object> orderMap = redissonClient.getMap(orderKey);

        if (orderMap.isExists()) {
            orderMap.put("currentStatus", orderStatus); // Update the status
            orderMap.put("updatedAt", LocalDateTime.now().toString()); // Update the updatedAt field
        }

    }


    public void flushAll() {
        RKeys keys = redissonClient.getKeys();
        keys.flushall();
    }
}
