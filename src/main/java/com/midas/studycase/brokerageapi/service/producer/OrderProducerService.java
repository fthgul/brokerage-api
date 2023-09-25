package com.midas.studycase.brokerageapi.service.producer;

import com.midas.studycase.brokerageapi.model.event.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderProducerService {

    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;

    public Mono<Void> sendOrderEvent(String topic, OrderEvent orderEvent) {
        return Mono.fromRunnable(() -> {
            try {
                kafkaTemplate.send(topic, orderEvent);
                log.info("Successfully sent message to Kafka topic: {}", topic);
            } catch (Exception e) {
                log.error("Error while sending message to Kafka", e);
                throw e;
            }
        });
    }
}
