package com.midas.studycase.brokerageapi.integration.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.midas.studycase.brokerageapi.TestBrokerageApiApplication;
import com.midas.studycase.brokerageapi.model.entity.*;
import com.midas.studycase.brokerageapi.model.enums.OrderStatus;
import com.midas.studycase.brokerageapi.model.enums.OrderType;
import com.midas.studycase.brokerageapi.model.event.OrderEvent;
import com.midas.studycase.brokerageapi.model.request.BuyOrderRequest;
import com.midas.studycase.brokerageapi.model.request.CancelOrderRequest;
import com.midas.studycase.brokerageapi.model.request.SellOrderRequest;
import com.midas.studycase.brokerageapi.repository.*;
import com.midas.studycase.brokerageapi.service.cache.OrderRedisReactiveService;
import com.midas.studycase.brokerageapi.service.cache.OrderRedisService;
import com.midas.studycase.brokerageapi.service.cache.StockCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest(classes = TestBrokerageApiApplication.class)
@AutoConfigureMockMvc
@Testcontainers
@TestPropertySource(locations = "classpath:application-test.properties")
class TradeControllerIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(TradeControllerIntegrationTest.class);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StockEntityRepository stockEntityRepository;

    @Autowired
    private UserStockEntityRepository userStockEntityRepository;

    @Autowired
    private OrderEntityRepository orderEntityRepository;

    @Autowired
    private OrderHistoryRepository orderHistoryRepository;

    @Autowired
    private StockCacheService stockCacheService;

    @Autowired
    private OrderRedisService orderRedisService;

    @Autowired
    private OrderRedisReactiveService orderRedisReactiveService;

    @Autowired
    UserEntityRepository userRepository;

    private static boolean isSetupDone = false;
    @BeforeEach
    void setUpOnce() {
        if (!isSetupDone) {
            logger.info("Setting up initial state before the first test execution");
            userStockEntityRepository.deleteAll();
            userRepository.deleteAll();
            isSetupDone = true;
        }
    }
    private void initializeStocks(String ticker, Integer quantity) {
        logger.info("Initializing stocks...");

        StockEntity stockEntity = stockEntityRepository.findByTicker(ticker);
        if (stockEntity == null) {
            stockEntity = new StockEntity();
            stockEntity.setTicker(ticker);
        }
        stockEntity.setQuantity(quantity);
        stockEntityRepository.save(stockEntity);

        logger.info("Stocks have been initialized.");
    }
    private SellOrderRequest createSellOrderRequest(Long userId) {
        logger.info("Creating sell order request...");

        SellOrderRequest request = new SellOrderRequest();
        request.setUserId(userId);
        request.setOrderId(UUID.randomUUID().toString());
        request.setQuantity(3);
        request.setTicker(UUID.randomUUID() + "APPL");

        logger.info("Sell order request has been created.");
        return request;
    }
    private CancelOrderRequest createCancelOrderRequest(Long userId) {
        logger.info("Creating cancel order request...");

        CancelOrderRequest request = new CancelOrderRequest();
        request.setUserId(userId);
        request.setTicker(UUID.randomUUID() + "APPL");
        request.setOrderId(UUID.randomUUID().toString());

        logger.info("Cancel order request has been created.");
        return request;
    }
    private BuyOrderRequest createBuyOrderRequest(Long userId) {
        logger.info("Creating buy order request...");

        BuyOrderRequest request = new BuyOrderRequest();
        request.setOrderId(UUID.randomUUID().toString());
        request.setUserId(userId);
        request.setQuantity(5);
        request.setTicker(UUID.randomUUID() + "APPL");

        logger.info("Buy order request has been created.");
        return request;
    }
    @Test
    @DisplayName("Given a BuyOrderRequest, when a buy order is made, then it should process the buy order correctly")
    void testBuyOrder() throws Exception {
        UserEntity userEntity = saveUser(new Random().nextInt());

        logger.info("Starting testBuyOrder test");
        BuyOrderRequest request = createBuyOrderRequest(userEntity.getId());

        //setup
        initializeStocks(request.getTicker(), request.getQuantity() + 2);

        performMockMvcPostRequest("/trades/buy", request);

        verifyBuyOrderProcessing(request , request.getQuantity() + 2);

        logger.info("testBuyOrder test completed");
    }
    @Test
    @DisplayName("Given a SellOrderRequest, when a sell order is made, then it should process the sell order correctly")
    void testSellOrder() throws Exception {
        logger.info("Starting testSellOrder test");
        //setup
        UserEntity userEntity = saveUser(new Random().nextInt());
        SellOrderRequest request = createSellOrderRequest(userEntity.getId());
        initializeStocks(request.getTicker(), request.getQuantity() + 2);
        saveUserStock(userEntity.getId(), request.getQuantity(), request.getTicker());

        performMockMvcPostRequest("/trades/sell", request);

        verifySellOrderProcessing(request, request.getQuantity() + 2);

        logger.info("testSellOrder test completed");
    }
    @Test
    @DisplayName("Given a CancelOrderRequest, when a cancel order is made, then it should process the cancel order correctly")
    void testCancelOrder() throws Exception {
        logger.info("Starting testCancelOrder test");
        UserEntity userEntity = saveUser(new Random().nextInt());
        CancelOrderRequest request = createCancelOrderRequest(userEntity.getId());
        initializeStocks(request.getTicker(), 2);
        saveUserStock(request.getUserId(), 2, request.getTicker());

        assumeOrderIsInCacheAndNotProcessed(request);

        performMockMvcPostRequest("/trades/cancel", request);

        verifyCancelOrderProcessing(request, 2);

        logger.info("testCancelOrder test completed");
    }
    private void performMockMvcPostRequest(String url, Object request) throws Exception {
        mockMvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted());
        Thread.sleep(1000); // Consider using Awaitility instead of Thread.sleep for better test stability.
    }
    private void assumeOrderIsInCacheAndNotProcessed(CancelOrderRequest request) {
        logger.info("Assuming order is in cache and not processed...");

        OrderEvent orderEvent = new OrderEvent();
        orderEvent.setOrderId(request.getOrderId());
        orderEvent.setUserId(request.getUserId());
        orderEvent.setOrderType(OrderType.BUY);
        orderEvent.setQuantity(new Random().nextInt());
        orderEvent.setTicker(request.getTicker());
        orderEvent.setCreatedAt(LocalDateTime.now());

        // Assuming the order is in cache and not processed yet
        orderRedisReactiveService.cacheOrder(orderEvent, OrderStatus.CREATED).block();

        logger.info("Order is assumed to be in cache and not processed.");
    }
    private void verifyBuyOrderProcessing(BuyOrderRequest request, int initStockQuantity) {
        logger.info("Verifying buy order processing for request: {}", request);

        // Verify the stock entity
        final StockEntity stockEntity = stockEntityRepository.findByTicker(request.getTicker());
        assertThat(stockEntity.getQuantity()).isEqualTo(initStockQuantity - request.getQuantity());

        // Verify the user stock entity
        final UserStockEntity userStock = userStockEntityRepository.findByUserIdAndTicker(request.getUserId(), request.getTicker());
        assertThat(userStock.getQuantity()).isEqualTo(request.getQuantity());

        // Verify the order entity
        final OrderEntity order = orderEntityRepository.findByOrderIdWithHistories(request.getOrderId()).orElseThrow();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);

        // Verify the order history entity
        OrderHistoryEntity orderHistoryEntity = orderHistoryRepository.findByOrderId(request.getOrderId()).orElseThrow();
        assertThat(orderHistoryEntity.getOrderType()).isEqualTo(OrderType.BUY);

        // Verify the cached stock
        final Integer cachedStock = stockCacheService.getCachedStock(request.getTicker());
        assertThat(cachedStock).isEqualTo(initStockQuantity - request.getQuantity());

        logger.info("Buy order processing verification completed for request: {}", request);
    }
    private void verifySellOrderProcessing(SellOrderRequest request, int initStockQuantity) {
        logger.info("Verifying sell order processing for request: {}", request);

        // Verify the stock entity
        final StockEntity stockEntity = stockEntityRepository.findByTicker(request.getTicker());
        assertThat(stockEntity.getQuantity()).isEqualTo(initStockQuantity + request.getQuantity());

        // Verify the user stock entity
        final UserStockEntity userStock = userStockEntityRepository.findByUserIdAndTicker(request.getUserId(), request.getTicker());
        assertThat(userStock.getQuantity()).isEqualTo(0);

        // Verify the order entity
        final OrderEntity order = orderEntityRepository.findByOrderIdWithHistories(request.getOrderId()).orElseThrow();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);

        // Verify the order history entity
        OrderHistoryEntity orderHistoryEntity = orderHistoryRepository.findByOrderId(request.getOrderId()).orElseThrow();
        assertThat(orderHistoryEntity.getOrderType()).isEqualTo(OrderType.SELL);

        // Verify the cached stock
        final Integer cachedStock = stockCacheService.getCachedStock(request.getTicker());
        assertThat(cachedStock).isEqualTo(initStockQuantity + request.getQuantity());

        logger.info("Sell order processing verification completed for request: {}", request);
    }
    private void verifyCancelOrderProcessing(CancelOrderRequest request, int initQuantity) {
        logger.info("Verifying cancel order processing for request: {}", request);

        // Verify the stock entity
        final StockEntity stockEntity = stockEntityRepository.findByTicker(request.getTicker());
        assertThat(stockEntity.getQuantity()).isEqualTo(initQuantity);


        // Verify the cancelled order in cache
        assertTrue(orderRedisService.isCancelledOrderInCache(request.getOrderId()));

        logger.info("Cancel order processing verification completed for request: {}", request);
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
