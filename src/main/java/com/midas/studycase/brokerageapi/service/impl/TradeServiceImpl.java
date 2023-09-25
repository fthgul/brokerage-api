package com.midas.studycase.brokerageapi.service.impl;

import com.midas.studycase.brokerageapi.config.kafka.KafkaConfig;
import com.midas.studycase.brokerageapi.model.enums.OrderStatus;
import com.midas.studycase.brokerageapi.model.enums.OrderType;
import com.midas.studycase.brokerageapi.model.event.OrderEvent;
import com.midas.studycase.brokerageapi.model.request.BuyOrderRequest;
import com.midas.studycase.brokerageapi.model.request.CancelOrderRequest;
import com.midas.studycase.brokerageapi.model.request.OrderRequest;
import com.midas.studycase.brokerageapi.model.request.SellOrderRequest;
import com.midas.studycase.brokerageapi.model.response.OrderResponse;
import com.midas.studycase.brokerageapi.service.TradeService;
import com.midas.studycase.brokerageapi.service.cache.OrderRedisReactiveService;
import com.midas.studycase.brokerageapi.service.producer.OrderProducerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TradeServiceImpl implements TradeService {

    private final OrderRedisReactiveService orderRedisReactiveService;
    private final OrderProducerService orderProducerService;
    @Override
    public Mono<OrderResponse> processBuyOrder(BuyOrderRequest order) {
        return processOrderEvent(order, OrderType.BUY, KafkaConfig.BUY_ORDERS_TOPIC);
    }

    @Override
    public Mono<OrderResponse> processSellOrder(SellOrderRequest order) {
        return processOrderEvent(order, OrderType.SELL, KafkaConfig.SELL_ORDERS_TOPIC);
    }

    @Override
    public Mono<OrderResponse> processCancelOrder(CancelOrderRequest order) {
        return processOrderEvent(order, OrderType.CANCEL, KafkaConfig.CANCELLED_ORDERS_TOPIC);
    }

    /**
     * Processes an order based on the provided order type and sends it to the specified Kafka topic.
     * <p>
     * This method performs the following steps:
     * 1. Prepares the order event based on the provided order request and order type.
     * 2. Attempts to add the order to a Redis queue.
     * 3. If successful, sends the order event to the specified Kafka topic.
     * 4. In case of any error (either in Redis or Kafka operations), it attempts to remove the order from the Redis queue.
     * </p>
     *
     * @param order     The order request containing details of the order.
     * @param orderType The type of the order (e.g., BUY, SELL, CANCEL).
     * @param topic     The Kafka topic to which the order event should be sent.
     * @return A {@link Mono<Void>} indicating the completion of the process.
     */
    private Mono<OrderResponse> processOrderEvent(OrderRequest order, OrderType orderType, String topic) {
        return prepareOrderEvent(order, orderType)
                .flatMap(orderEvent -> processOrderEvent(orderEvent, topic));

    }

    /**
     * Processes a sell or buy order by:
     * 1. Caching it in Redis.
     * 2. Sending the order event to the specified topic.
     *
     * If the order is successfully processed, it returns an OrderResponse containing the transactionId and orderId.
     * If there's an error during the process, it attempts to remove the order from the Redis cache and throws an exception.
     *
     * @param orderEvent The order event containing details of the order.
     * @param topic      The topic to which the order event should be sent.
     * @return A Mono<OrderResponse> containing the transactionId and orderId of the processed order.
     */
    private Mono<OrderResponse> processOrderEvent(OrderEvent orderEvent, String topic) {
        log.info("Processing order with ID: {}", orderEvent.getOrderId());

        return orderRedisReactiveService.cacheOrder(orderEvent, OrderStatus.CREATED)
                .flatMap(success -> {
                    if (!success) {
                        log.error("Failed to write order with ID: {} to Redis", orderEvent.getOrderId());
                        return Mono.error(new RuntimeException("Failed to write to Redis"));
                    }

                    log.info("Successfully cached order with ID: {} to Redis", orderEvent.getOrderId());
                    return orderProducerService.sendOrderEvent(topic, orderEvent);
                })
                .then(Mono.fromCallable(() -> new OrderResponse(orderEvent.getOrderId())
                ))
                .onErrorResume(e -> {
                    log.error("Error occurred during order processing for order ID: {}", orderEvent.getOrderId(), e);
                    return orderRedisReactiveService.removeCachedOrder(orderEvent.getOrderId(), orderEvent.getUserId())
                            .then(Mono.error(new RuntimeException("An error occurred while processing the order. Please try again later.")));
                });
    }



    /**
     * Prepares an OrderEvent based on the given OrderRequest and OrderType.
     *
     * @param orderRequest The order request containing details of the order.
     * @param orderType    The type of the order (e.g., BUY, SELL, CANCEL).
     * @return The prepared OrderEvent.
     */
    private Mono<OrderEvent> prepareOrderEvent(OrderRequest orderRequest, OrderType orderType) {
        OrderEvent orderEvent = new OrderEvent();

        //set common info
        orderEvent.setUserId(orderRequest.getUserId());
        orderEvent.setOrderType(orderType);
        orderEvent.setCreatedAt(LocalDateTime.now());


        if (orderRequest instanceof BuyOrderRequest) {
            populateOrderEventFromBuyRequest(orderEvent, (BuyOrderRequest) orderRequest);
        } else if (orderRequest instanceof SellOrderRequest) {
            populateOrderEventFromSellRequest(orderEvent, (SellOrderRequest) orderRequest);
        } else if (orderRequest instanceof CancelOrderRequest) {
            populateOrderEventFromCancelRequest(orderEvent, (CancelOrderRequest) orderRequest);
        }

        if (orderEvent.getOrderId() == null || orderEvent.getOrderId().isEmpty()) {
            orderEvent.setOrderId(UUID.randomUUID().toString());
        }

        log.debug("Order event is prepared: {}", orderEvent);
        return Mono.just(orderEvent);
    }

    private void populateOrderEventFromCancelRequest(OrderEvent orderEvent, CancelOrderRequest cancelOrderRequest) {
        orderEvent.setOrderId(cancelOrderRequest.getOrderId());
        orderEvent.setTicker(cancelOrderRequest.getTicker());
    }




    private void populateOrderEventFromBuyRequest(OrderEvent orderEvent, BuyOrderRequest buyOrderRequest) {
        orderEvent.setOrderId(buyOrderRequest.getOrderId());
        orderEvent.setQuantity(buyOrderRequest.getQuantity());
        orderEvent.setTicker(buyOrderRequest.getTicker());
    }

    private void populateOrderEventFromSellRequest(OrderEvent orderEvent, SellOrderRequest sellOrderRequest) {
        orderEvent.setOrderId(sellOrderRequest.getOrderId());
        orderEvent.setQuantity(sellOrderRequest.getQuantity());
        orderEvent.setTicker(sellOrderRequest.getTicker());
    }

}
