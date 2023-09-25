package com.midas.studycase.brokerageapi.integration.service.cache;

import com.midas.studycase.brokerageapi.integration.config.RedisContainerConfig;
import com.midas.studycase.brokerageapi.integration.config.TestConfigRedis;
import com.midas.studycase.brokerageapi.integration.config.property.RedisProperties;
import com.midas.studycase.brokerageapi.model.enums.OrderStatus;
import com.midas.studycase.brokerageapi.model.enums.OrderType;
import com.midas.studycase.brokerageapi.model.event.OrderEvent;
import com.midas.studycase.brokerageapi.service.cache.OrderRedisReactiveService;
import com.midas.studycase.brokerageapi.service.cache.OrderRedisService;
import org.junit.jupiter.api.*;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.UUID;

@ActiveProfiles("test")
@Testcontainers
@SpringBootTest(classes = {OrderRedisService.class, OrderRedisReactiveService.class, TestConfigRedis.class, RedisProperties.class, RedisContainerConfig.class})
@TestPropertySource(locations = "classpath:application-test.properties")
public class OrderRedisServiceIntegrationTest {

    @Autowired
    private OrderRedisService orderRedisService;

    @Autowired
    private OrderRedisReactiveService orderRedisReactiveService;

    @Autowired
    private RedissonClient redissonClient;

    @BeforeEach
    public void setUp() {
        orderRedisService.flushAll();
    }

    @Test
    @DisplayName("Should Detect Cancelled Order In Cache")
    public void shouldDetectCancelledOrderInCache() {
        // Given
        String orderId = UUID.randomUUID().toString();
        final OrderEvent orderEvent = buildOrderEvent(orderId);
        orderRedisReactiveService.cacheOrder(orderEvent, OrderStatus.COMPLETED).block();
        orderRedisService.updateOrderStatusInCache(orderId, OrderStatus.CANCELLED);

        // When & Then
        Assertions.assertTrue(orderRedisService.isCancelledOrderInCache(orderId));
    }

    @Test
    @DisplayName("Should Not Detect Cancelled Order In Cache")
    public void shouldNotDetectCancelledOrderInCache() {
        // Given
        String orderId = UUID.randomUUID().toString();
        final OrderEvent orderEvent = buildOrderEvent(orderId);
        orderRedisReactiveService.cacheOrder(orderEvent, OrderStatus.COMPLETED).block();

        // When & Then
        Assertions.assertFalse(orderRedisService.isCancelledOrderInCache(orderId));
    }

    @Test
    @DisplayName("Should Update Order Status In Cache")
    public void shouldUpdateOrderStatusInCache() {
        // Given
        String orderId = UUID.randomUUID().toString();
        OrderStatus initialStatus = OrderStatus.CREATED;
        OrderStatus updatedStatus = OrderStatus.CANCELLED;
        final OrderEvent orderEvent = buildOrderEvent(orderId);
        orderRedisReactiveService.cacheOrder(orderEvent, initialStatus).block();

        // When
        orderRedisService.updateOrderStatusInCache(orderId, updatedStatus);

        // Then
        String orderKey = orderRedisService.generateOrderKey(orderId);
        RMap<String, Object> orderMap = redissonClient.getMap(orderKey);
        Assertions.assertTrue(orderMap.isExists());
        Assertions.assertEquals(updatedStatus.name(), orderMap.get("currentStatus"));
    }

    @Test
    @DisplayName("Should Return False When Order Is Not In Cache")
    public void shouldReturnFalseWhenOrderIsNotInCache() {
        // Given
        String nonExistentOrderId = UUID.randomUUID().toString();

        // When & Then
        Assertions.assertFalse(orderRedisService.isCancelledOrderInCache(nonExistentOrderId));
    }

    @Test
    @DisplayName("Should Not Update Status For Non-Existent Order In Cache")
    public void shouldNotUpdateStatusForNonExistentOrderInCache() {
        // Given
        String nonExistentOrderId = UUID.randomUUID().toString();
        OrderStatus updatedStatus = OrderStatus.CANCELLED;

        // When
        orderRedisService.updateOrderStatusInCache(nonExistentOrderId, updatedStatus);

        // Then
        String orderKey = orderRedisService.generateOrderKey(nonExistentOrderId);
        RMap<String, Object> orderMap = redissonClient.getMap(orderKey);
        Assertions.assertFalse(orderMap.isExists()); // Expecting that the orderMap does not exist as the order is non-existent
    }

    @Test
    @DisplayName("Should Not Update Status If Order Status Is Already Cancelled In Cache")
    public void shouldNotUpdateStatusIfOrderStatusIsAlreadyCancelledInCache() {
        // Given
        String orderId = UUID.randomUUID().toString();
        OrderStatus initialStatus = OrderStatus.CANCELLED;
        final OrderEvent orderEvent = buildOrderEvent(orderId);
        orderRedisReactiveService.cacheOrder(orderEvent, initialStatus).block();

        // When
        orderRedisService.updateOrderStatusInCache(orderId, initialStatus); // Trying to update to the same status

        // Then
        String orderKey = orderRedisService.generateOrderKey(orderId);
        RMap<String, Object> orderMap = redissonClient.getMap(orderKey);
        Assertions.assertTrue(orderMap.isExists());
        Assertions.assertEquals(initialStatus.name(), orderMap.get("currentStatus")); // Expecting that the status has not changed
    }

    private OrderEvent buildOrderEvent(String orderId) {
        OrderEvent orderEvent = new OrderEvent();
        orderEvent.setOrderId(orderId);
        orderEvent.setUserId(1L); // Consider refactoring to avoid hardcoding.
        orderEvent.setOrderType(OrderType.BUY);
        orderEvent.setQuantity(5); // Consider refactoring to avoid hardcoding.
        orderEvent.setTicker("APPL"); // Consider refactoring to avoid hardcoding.
        orderEvent.setCreatedAt(LocalDateTime.now());
        return orderEvent;
    }

}
