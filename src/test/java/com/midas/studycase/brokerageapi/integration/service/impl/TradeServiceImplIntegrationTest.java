package com.midas.studycase.brokerageapi.integration.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.midas.studycase.brokerageapi.TestBrokerageApiApplication;
import com.midas.studycase.brokerageapi.config.kafka.KafkaConfig;
import com.midas.studycase.brokerageapi.integration.config.TestConfigKafka;
import com.midas.studycase.brokerageapi.integration.util.KafkaTestUtils;
import com.midas.studycase.brokerageapi.model.enums.OrderType;
import com.midas.studycase.brokerageapi.model.event.OrderEvent;
import com.midas.studycase.brokerageapi.model.request.BuyOrderRequest;
import com.midas.studycase.brokerageapi.model.request.CancelOrderRequest;
import com.midas.studycase.brokerageapi.model.request.OrderRequest;
import com.midas.studycase.brokerageapi.model.request.SellOrderRequest;
import com.midas.studycase.brokerageapi.service.TradeService;
import com.midas.studycase.brokerageapi.service.cache.OrderRedisService;
import com.midas.studycase.brokerageapi.service.consumer.OrderConsumerService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;


@Slf4j
@ActiveProfiles("test")
@SpringBootTest(classes = {TestBrokerageApiApplication.class})
@Testcontainers
@TestPropertySource(locations = "classpath:application-test.properties")
public class TradeServiceImplIntegrationTest {

    @Autowired
    OrderRedisService orderRedisService;

    @Autowired
    TradeService tradeService;

    @Autowired
    TestConfigKafka testConfigKafka;

    private static ObjectMapper objectMapper = new ObjectMapper();


    @MockBean
    private OrderConsumerService orderConsumerService;


    @BeforeAll
    static void setup() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        log.info("ObjectMapper has been set up.");
    }

    @Test
    @DisplayName("Given a BuyOrderRequest, when processBuyOrder is called, then it should complete successfully")
    public void shouldProcessBuyOrderSuccessfully() throws IOException {
        log.info("Executing shouldProcessBuyOrderSuccessfully");

        // Given
        BuyOrderRequest buyOrderRequest = createBuyOrderRequest();

        // When
        StepVerifier.create(tradeService.processBuyOrder(buyOrderRequest))
                .expectNextCount(1)
                .verifyComplete();

        // Then
        Consumer<String, String> consumer = createConsumer(KafkaConfig.BUY_ORDERS_TOPIC, UUID.randomUUID().toString());
        verifyOrderEvent(consumer, buyOrderRequest, OrderType.BUY);

        log.info("shouldProcessBuyOrderSuccessfully has been executed successfully");
    }

    @Test
    @DisplayName("Given a SellOrderRequest, when processSellOrder is called, then it should complete successfully")
    public void shouldProcessSellOrderSuccessfully() throws IOException {
        log.info("Executing shouldProcessSellOrderSuccessfully");

        // Given
        SellOrderRequest sellOrderRequest = createSellOrderRequest();

        // When
        StepVerifier.create(tradeService.processSellOrder(sellOrderRequest))
                .expectNextCount(1)
                .verifyComplete();

        // Then
        Consumer<String, String> consumer = createConsumer(KafkaConfig.SELL_ORDERS_TOPIC, UUID.randomUUID().toString());
        verifyOrderEvent(consumer, sellOrderRequest, OrderType.SELL);

        log.info("shouldProcessSellOrderSuccessfully has been executed successfully");
    }

    @Test
    @DisplayName("Given a CancelOrderRequest, when processCancelOrder is called, then it should complete successfully")
    public void shouldProcessCancelOrderSuccessfully() throws IOException {
        log.info("Executing shouldProcessCancelOrderSuccessfully");

        // Given
        CancelOrderRequest cancelOrderRequest = createCancelOrderRequest();

        // When
        StepVerifier.create(tradeService.processCancelOrder(cancelOrderRequest))
                .expectNextCount(1)
                .verifyComplete();

        // Then
        Consumer<String, String> consumer = createConsumer(KafkaConfig.CANCELLED_ORDERS_TOPIC, UUID.randomUUID().toString());
        verifyOrderEvent(consumer, cancelOrderRequest, OrderType.CANCEL);

        log.info("shouldProcessCancelOrderSuccessfully has been executed successfully");
    }

    private BuyOrderRequest createBuyOrderRequest() {
        log.info("Creating BuyOrderRequest");
        BuyOrderRequest buyOrderRequest = new BuyOrderRequest();
        buyOrderRequest.setOrderId(UUID.randomUUID().toString());
        buyOrderRequest.setUserId(new Random().nextLong());
        buyOrderRequest.setTicker(UUID.randomUUID() + "APPL");
        buyOrderRequest.setQuantity(5);
        log.info("BuyOrderRequest has been created: {}", buyOrderRequest);
        return buyOrderRequest;
    }

    private SellOrderRequest createSellOrderRequest() {
        log.info("Creating SellOrderRequest");
        SellOrderRequest sellOrderRequest = new SellOrderRequest();
        sellOrderRequest.setOrderId(UUID.randomUUID().toString());
        sellOrderRequest.setUserId(new Random().nextLong());
        sellOrderRequest.setTicker(UUID.randomUUID() + "APPL");
        sellOrderRequest.setQuantity(3);
        log.info("SellOrderRequest has been created: {}", sellOrderRequest);
        return sellOrderRequest;
    }

    private CancelOrderRequest createCancelOrderRequest() {
        log.info("Creating CancelOrderRequest");
        CancelOrderRequest cancelOrderRequest = new CancelOrderRequest();
        cancelOrderRequest.setOrderId(UUID.randomUUID().toString());
        cancelOrderRequest.setTicker(UUID.randomUUID() + "APPL");
        cancelOrderRequest.setUserId(new Random().nextLong());
        log.info("CancelOrderRequest has been created: {}", cancelOrderRequest);
        return cancelOrderRequest;
    }

    private Consumer<String, String> createConsumer(String topic, String groupSuffix) {
        log.info("Creating Kafka Consumer for topic: {}", topic);
        Consumer<String, String> consumer = KafkaTestUtils.createConsumer(
                testConfigKafka.getKafkaProperties().getBootstrapServers(),
                "testGroup"+ groupSuffix, topic);
        log.info("Kafka Consumer has been created for topic: {}", topic);
        return consumer;
    }

    private void verifyOrderEvent(Consumer<String, String> consumer, OrderRequest orderRequest, OrderType orderType) throws IOException {
        log.info("Verifying OrderEvent for OrderRequest: {}", orderRequest);
        ConsumerRecords<String, String> records = KafkaTestUtils.getRecords(consumer);
        assertEquals(1, records.count()); // Ensure we have consumed one message

        String consumedMessage = records.iterator().next().value();
        OrderEvent consumedOrderEvent = objectMapper.readValue(consumedMessage, OrderEvent.class);

        Assertions.assertThat(consumedOrderEvent.getOrderType()).isEqualTo(orderType);
        Assertions.assertThat(consumedOrderEvent.getUserId()).isEqualTo(orderRequest.getUserId());
        log.info("OrderEvent has been verified successfully for OrderRequest: {}", orderRequest);
    }
}
