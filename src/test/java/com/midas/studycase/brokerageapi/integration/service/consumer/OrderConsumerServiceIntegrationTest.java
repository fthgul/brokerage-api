package com.midas.studycase.brokerageapi.integration.service.consumer;

import com.midas.studycase.brokerageapi.TestBrokerageApiApplication;
import com.midas.studycase.brokerageapi.config.kafka.KafkaConfig;
import com.midas.studycase.brokerageapi.model.entity.*;
import com.midas.studycase.brokerageapi.model.enums.OrderStatus;
import com.midas.studycase.brokerageapi.model.enums.OrderType;
import com.midas.studycase.brokerageapi.model.event.OrderEvent;
import com.midas.studycase.brokerageapi.model.request.CancelOrderRequest;
import com.midas.studycase.brokerageapi.repository.*;
import com.midas.studycase.brokerageapi.service.cache.OrderRedisReactiveService;
import com.midas.studycase.brokerageapi.service.cache.OrderRedisService;
import com.midas.studycase.brokerageapi.service.cache.StockCacheService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
@ActiveProfiles("test")
@SpringBootTest(classes = TestBrokerageApiApplication.class)
@Testcontainers
@TestPropertySource(locations = "classpath:application-test.properties")
public class OrderConsumerServiceIntegrationTest {

    @Autowired
    private KafkaTemplate<String, OrderEvent> kafkaTemplate;

    @Autowired
    UserStockEntityRepository userStockEntityRepository;

    @Autowired
    StockEntityRepository stockEntityRepository;

    @Autowired
    OrderHistoryRepository orderHistoryRepository;

    @Autowired
    OrderRedisService orderRedisService;

    @Autowired
    OrderRedisReactiveService orderRedisReactiveService;

    @Autowired
    OrderEntityRepository orderRepository;

    @Autowired
    StockCacheService stockCacheService;

    @Autowired
    UserEntityRepository userRepository;

    private static boolean isSetupDone = false;

    @BeforeEach
    public void setup() {
        if (!isSetupDone) {
            log.info("Setting up initial state before the first test execution");
            userStockEntityRepository.deleteAll();
            userRepository.deleteAll();
            isSetupDone = true;
        }
    }

    private void initializeStocks(String ticker, Integer quantity) {
        log.info("Initializing stocks...");

        StockEntity stockEntity = stockEntityRepository.findByTicker(ticker);
        if (stockEntity == null) {
            stockEntity = new StockEntity();
            stockEntity.setTicker(ticker);
        }
        stockEntity.setQuantity(quantity);
        stockEntityRepository.save(stockEntity);

        log.info("Stocks have been initialized.");
    }

    /*@Test
    @DisplayName("Should Consume Buy Order Event and Validate Entities")
    public void shouldConsumeBuyOrderEvent() throws InterruptedException {
        // Given
        final UserEntity userEntity = saveUser(new Random().nextInt());
        OrderEvent orderEvent = buildOrderEvent(UUID.randomUUID().toString(), userEntity.getId(), OrderType.BUY);
        initializeStocks(orderEvent.getTicker(), orderEvent.getQuantity());
        saveUserStock(userEntity.getId(), orderEvent.getQuantity(), orderEvent.getTicker() );
        orderRedisReactiveService.cacheOrder(orderEvent, OrderStatus.CREATED).block();
        kafkaTemplate.send(KafkaConfig.BUY_ORDERS_TOPIC, orderEvent);

        // When
        Thread.sleep(10000); // Simulating processing delay

        // Then
        validateEntitiesAfterBuyOrder(orderEvent);
    }*/

    /*@Test
    @DisplayName("Should Consume Sell Order Event and Validate Entities")
    public void shouldConsumeSellOrderEvent() throws InterruptedException {
        // Given
        final UserEntity userEntity = saveUser(new Random().nextInt());
        OrderEvent orderEvent = buildOrderEvent(UUID.randomUUID().toString(), userEntity.getId(), OrderType.SELL);
        initializeStocks(orderEvent.getTicker(), orderEvent.getQuantity());
        saveUserStock(userEntity.getId(), orderEvent.getQuantity(), orderEvent.getTicker());


        orderRedisReactiveService.cacheOrder(orderEvent, OrderStatus.CREATED).block();
        kafkaTemplate.send(KafkaConfig.SELL_ORDERS_TOPIC, orderEvent);

        // When
        Thread.sleep(10000); // Simulating processing delay

        // Then
        validateEntitiesAfterSellOrder(orderEvent);
    }*/

    private void validateEntitiesAfterBuyOrder(OrderEvent orderEvent) {
        final StockEntity stock = stockEntityRepository.findByTicker(orderEvent.getTicker());
        assertEquals(0, stock.getQuantity());

        final UserStockEntity userStock = userStockEntityRepository.findByUserIdAndTicker(orderEvent.getUserId(), orderEvent.getTicker());
        assertEquals(orderEvent.getQuantity()*2, userStock.getQuantity());

        OrderHistoryEntity orderHistoryEntity = orderHistoryRepository.findByOrderId(orderEvent.getOrderId()).get();
        assertEquals(OrderType.BUY, orderHistoryEntity.getOrderType());

        final OrderEntity order = orderRepository.findByOrderIdWithHistories(orderEvent.getOrderId()).get();
        assertEquals(OrderStatus.COMPLETED, order.getStatus());
    }

    private void validateEntitiesAfterSellOrder(OrderEvent orderEvent) {
        final StockEntity stock = stockEntityRepository.findByTicker(orderEvent.getTicker());
        assertEquals(orderEvent.getQuantity()*2, stock.getQuantity());

        final UserStockEntity userStock = userStockEntityRepository.findByUserIdAndTicker(orderEvent.getUserId(), orderEvent.getTicker());
        assertEquals(0, userStock.getQuantity());

        OrderHistoryEntity orderHistoryEntity = orderHistoryRepository.findByOrderId(orderEvent.getOrderId()).get();
        assertEquals(OrderType.SELL, orderHistoryEntity.getOrderType());

        final OrderEntity order = orderRepository.findByOrderIdWithHistories(orderEvent.getOrderId()).get();
        assertEquals(OrderStatus.COMPLETED, order.getStatus());
    }

    private void validateEntitiesAfterCancelOrder(CancelOrderRequest request) {
        final StockEntity stock = stockEntityRepository.findByTicker("APPL");
        assertEquals(5, stock.getQuantity());

        final UserStockEntity userStock = userStockEntityRepository.findByUserIdAndTicker(1L, "APPL");
        assertEquals(4, userStock.getQuantity());

        assertTrue(orderRedisService.isCancelledOrderInCache(request.getOrderId()));
    }

    private OrderEvent buildOrderEvent(String orderId, Long userId, OrderType orderType) {
        OrderEvent orderEvent = new OrderEvent();
        orderEvent.setOrderId(orderId);
        orderEvent.setUserId(userId);
        orderEvent.setOrderType(orderType);
        orderEvent.setQuantity(5);
        orderEvent.setTicker(UUID.randomUUID() + "APPL");
        orderEvent.setCreatedAt(LocalDateTime.now());
        return orderEvent;
    }

    private UserEntity saveUser(int i) {
        UserEntity userEntity = new UserEntity();
        userEntity.setEmail(i + "email@test.com");
        userEntity.setUsername(String.valueOf(i));
        userRepository.save(userEntity);
        return userEntity;
    }
    private void saveUserStock(Long userId, int quantity, String ticker) {
        UserStockEntity userStockEntity = new UserStockEntity();
        userStockEntity.setUserId(userId);
        userStockEntity.setTicker(ticker);
        userStockEntity.setQuantity(quantity);
        userStockEntityRepository.save(userStockEntity);
    }
}
