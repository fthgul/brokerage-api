package com.midas.studycase.brokerageapi.service.consumer;

import com.midas.studycase.brokerageapi.config.kafka.KafkaConfig;
import com.midas.studycase.brokerageapi.model.event.OrderEvent;
import com.midas.studycase.brokerageapi.service.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderConsumerService {

    private final StockService stockService;

    /**
     * Listens to the Kafka topics for order events and processes them accordingly.
     *
     * @param orderEvent The order event message from Kafka.
     * @param ack        The acknowledgment for Kafka message processing.
     */
    @KafkaListener(topics = {KafkaConfig.BUY_ORDERS_TOPIC, KafkaConfig.SELL_ORDERS_TOPIC, KafkaConfig.CANCELLED_ORDERS_TOPIC}, groupId = KafkaConfig.STOCK_ACTION_CONSUMER_GROUP_ID)
    public void consumeOrderEvent(OrderEvent orderEvent, Acknowledgment ack) {
        log.debug("Received order event for processing: {}", orderEvent);

        try {
            processOrderEvent(orderEvent);
            ack.acknowledge();
            log.info("Successfully processed and acknowledged order event: {}", orderEvent);
        } catch (Exception e) {
            log.error("An unexpected error occurred while processing the order event message: {}", orderEvent, e);
        }
    }

    /**
     * Processes the order event based on its type.
     *
     * @param orderEvent The order event to be processed.
     */
    private void processOrderEvent(OrderEvent orderEvent) {

        if (orderEvent.getOrderType() == null) {
            throw new IllegalArgumentException("Order type cannot be null for order: " + orderEvent.getOrderId());
        }

        switch (orderEvent.getOrderType()) {
            case BUY -> stockService.processBuyOrder(orderEvent);
            case SELL -> stockService.processSellOrder(orderEvent);
            case CANCEL -> stockService.processCancelOrder(orderEvent);
            default -> throw new IllegalArgumentException("Unknown order type: " + orderEvent.getOrderType());
        }
    }
}
