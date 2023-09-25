package com.midas.studycase.brokerageapi.integration.service.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.midas.studycase.brokerageapi.integration.config.KafkaContainerConfig;
import com.midas.studycase.brokerageapi.config.kafka.KafkaConfig;
import com.midas.studycase.brokerageapi.config.kafka.KafkaProperties;
import com.midas.studycase.brokerageapi.integration.config.TestConfigKafka;
import com.midas.studycase.brokerageapi.integration.util.KafkaTestUtils;
import com.midas.studycase.brokerageapi.model.enums.OrderType;
import com.midas.studycase.brokerageapi.model.event.OrderEvent;
import com.midas.studycase.brokerageapi.service.producer.OrderProducerService;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(classes = {OrderProducerService.class, KafkaContainerConfig.class, TestConfigKafka.class, KafkaProperties.class, ObjectMapper.class})
@Testcontainers
@TestPropertySource(locations = "classpath:application-test.properties")
class OrderProducerServiceIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(OrderProducerServiceIntegrationTest.class);

    @Autowired
    private OrderProducerService orderProducerService;

    @Autowired
    private TestConfigKafka testConfigKafka;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        logger.info("ObjectMapper has been set up.");
    }

    @Test
    @DisplayName("Given an OrderEvent, when sendOrderEvent is called, then it should send the OrderEvent to the specified topic")
    void testSendOrderEvent() throws JsonProcessingException {
        logger.info("Starting testSendOrderEvent test");

        OrderEvent orderEvent = createOrderEvent();

        logger.info("Sending OrderEvent to the topic: {}", KafkaConfig.BUY_ORDERS_TOPIC);
        orderProducerService.sendOrderEvent(KafkaConfig.BUY_ORDERS_TOPIC, orderEvent).block();

        Consumer<String, String> consumer = setupConsumer(KafkaConfig.BUY_ORDERS_TOPIC);

        ConsumerRecords<String, String> records = KafkaTestUtils.getRecords(consumer);
        assertThat(records.count()).isEqualTo(1); // Ensure we have consumed one message

        String consumedMessage = records.iterator().next().value();
        OrderEvent consumedOrderEvent = objectMapper.readValue(consumedMessage, OrderEvent.class);

        logger.info("Verifying the consumed OrderEvent: {}", consumedOrderEvent);
        assertThat(consumedOrderEvent).usingRecursiveComparison().isEqualTo(orderEvent);

        consumer.close();
        logger.info("testSendOrderEvent test completed");
    }

    private Consumer<String, String> setupConsumer(String topic) {
        logger.info("Setting up a Kafka consumer to consume the message from the topic: {}", topic);
        return KafkaTestUtils.createConsumer(testConfigKafka.getKafkaProperties().getBootstrapServers(),
                "testGroup", topic);
    }

    private OrderEvent createOrderEvent() {
        logger.info("Creating an OrderEvent");
        OrderEvent orderEvent = new OrderEvent();
        orderEvent.setOrderId(UUID.randomUUID().toString());
        orderEvent.setOrderType(OrderType.BUY);
        orderEvent.setQuantity(5);
        orderEvent.setTicker(UUID.randomUUID().toString().concat("APPLE"));
        orderEvent.setUserId(1L);
        return orderEvent;
    }
}
