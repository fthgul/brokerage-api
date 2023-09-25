package com.midas.studycase.brokerageapi.service.impl;

import com.midas.studycase.brokerageapi.exception.ExceedingSystemStockLimitException;
import com.midas.studycase.brokerageapi.exception.InsufficientStockException;
import com.midas.studycase.brokerageapi.exception.StockNotFoundException;
import com.midas.studycase.brokerageapi.model.entity.StockEntity;
import com.midas.studycase.brokerageapi.model.entity.UserStockEntity;
import com.midas.studycase.brokerageapi.model.enums.OrderStatus;
import com.midas.studycase.brokerageapi.model.event.OrderEvent;
import com.midas.studycase.brokerageapi.repository.StockEntityRepository;
import com.midas.studycase.brokerageapi.repository.UserStockEntityRepository;
import com.midas.studycase.brokerageapi.service.OrderHistoryService;
import com.midas.studycase.brokerageapi.service.OrderService;
import com.midas.studycase.brokerageapi.service.StockService;
import com.midas.studycase.brokerageapi.service.cache.OrderRedisService;
import com.midas.studycase.brokerageapi.service.cache.StockCacheService;
import com.midas.studycase.brokerageapi.service.producer.NotifyProducerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.concurrent.TimeUnit;


/**
 * Implementation of the StockService interface.
 * This service handles the main logic for processing stock orders.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockServiceImpl implements StockService {
    private static final String INSUFFICIENT_STOCK_MESSAGE = "Insufficient stock";
    private static final String STOCK_NOT_FOUND_MESSAGE = "Stock not found for ticker: ";

    private final StockEntityRepository stockRepository;
    private final UserStockEntityRepository userStockRepository;
    private final NotifyProducerService notifyService;
    private final OrderService orderService;
    private final OrderHistoryService orderHistoryService;
    private final StockCacheService stockCacheService;
    private final OrderRedisService orderRedisService;
    private final RedissonClient redissonClient;



    @Value("${system.stock.limit}")
    private int systemStockLimit;


    /**
     * Processes a buy order based on the provided order event. This method ensures that the order processing
     * is thread-safe by acquiring a distributed lock using Redisson. If the lock cannot be acquired within
     * a specified timeout, the order processing is aborted.
     *
     * <p>Once the lock is acquired, the method attempts to handle the buy order. If any specific exceptions
     * related to stock availability or stock not being found are encountered, they are handled using the
     * {@code handleOrderException} method. Any other unexpected exceptions are handled using the
     * {@code handleGenericException} method.</p>
     *
     * <p>After the order processing is completed or if any exceptions are encountered, the distributed lock
     * is released to allow other instances or threads to process other orders.</p>
     *
     * @param orderEvent The event containing details of the order to be processed.
     * @throws InsufficientStockException if there is not enough stock available for the order.
     * @throws StockNotFoundException if the stock related to the order is not found.
     */
    @Override
    @Transactional
    public void processBuyOrder(OrderEvent orderEvent) {
        String lockKey = "order:lock:" + orderEvent.getOrderId();
        RLock lock = redissonClient.getLock(lockKey);
        boolean isLockAcquired = false;

        try {
            isLockAcquired = lock.tryLock(10, TimeUnit.SECONDS);
            if (!isLockAcquired) {
                log.warn("Unable to acquire lock for order: {}. Order processing aborted.", orderEvent.getOrderId());
                return;
            }

            log.info("Processing buy order for user: {}. OrderId: {}", orderEvent.getUserId(), orderEvent.getOrderId());
            handleBuyOrder(orderEvent);
        } catch (InsufficientStockException | StockNotFoundException e) {
            log.error("Error processing order for user: {}. OrderId: {}. Reason: {}", orderEvent.getUserId(), orderEvent.getOrderId(), e.getMessage());
            handleOrderException(orderEvent, e);
        } catch (Exception e) {
            log.error("Unexpected error processing order for user: {}. OrderId: {}. Reason: {}", orderEvent.getUserId(), orderEvent.getOrderId(), e.getMessage());
            handleGenericException(orderEvent, e);
        } finally {
            if (isLockAcquired) {
                lock.unlock();
                log.info("Lock released for order: {}", orderEvent.getOrderId());
            }
        }
    }

    /**
     * Processes a sell order based on the provided order event. This method ensures that the order processing
     * is thread-safe by acquiring a distributed lock using Redisson. If the lock cannot be acquired within
     * a specified timeout, the order processing is aborted.
     *
     * <p>Once the lock is acquired, the method attempts to handle the sell order. If any specific exceptions
     * related to stock availability, stock not being found, or exceeding system stock limits are encountered,
     * they are handled using the {@code handleOrderException} method. Any other unexpected exceptions are
     * handled using the {@code handleGenericException} method.</p>
     *
     * <p>After the order processing is completed or if any exceptions are encountered, the distributed lock
     * is released to allow other instances or threads to process other orders.</p>
     *
     * @param orderEvent The event containing details of the order to be processed.
     * @throws InsufficientStockException if there is not enough stock available for the order.
     * @throws StockNotFoundException if the stock related to the order is not found.
     * @throws ExceedingSystemStockLimitException if the order exceeds the system's stock limit.
     */
    @Override
    @Transactional
    public void processSellOrder(OrderEvent orderEvent) {
        String lockKey = "order:lock:" + orderEvent.getOrderId();
        RLock lock = redissonClient.getLock(lockKey);
        boolean isLockAcquired = false;

        try {
            isLockAcquired = lock.tryLock(10, TimeUnit.SECONDS);
            if (!isLockAcquired) {
                log.warn("Unable to acquire lock for order: {}. Order processing aborted.", orderEvent.getOrderId());
                return;
            }

            log.info("Processing sell order for user: {}. OrderId: {}", orderEvent.getUserId(), orderEvent.getOrderId());
            handleSellOrder(orderEvent);
        } catch (InsufficientStockException | StockNotFoundException | ExceedingSystemStockLimitException e) {
            log.error("Error processing order for user: {}. OrderId: {}. Reason: {}", orderEvent.getUserId(), orderEvent.getOrderId(), e.getMessage());
            handleOrderException(orderEvent, e);
        } catch (Exception e) {
            log.error("Unexpected error processing order for user: {}. OrderId: {}. Reason: {}", orderEvent.getUserId(), orderEvent.getOrderId(), e.getMessage());
            handleGenericException(orderEvent, e);
        } finally {
            if (isLockAcquired) {
                lock.unlock();
                log.info("Lock released for order: {}", orderEvent.getOrderId());
            }
        }
    }

    /**
     * Processes a cancel order request based on the provided order event. This method ensures that the order cancellation
     * is thread-safe by acquiring a distributed lock using Redisson. If the lock cannot be acquired within
     * a specified timeout, the order cancellation is aborted.
     *
     * <p>Once the lock is acquired, the method attempts to handle the cancel order request. Any unexpected exceptions
     * encountered during the cancellation process are handled using the {@code handleGenericException} method.</p>
     *
     * <p>After the order cancellation is completed or if any exceptions are encountered, the distributed lock
     * is released to allow other instances or threads to process other orders or cancellations.</p>
     *
     * @param orderEvent The event containing details of the order to be cancelled.
     */
    @Override
    @Transactional
    public void processCancelOrder(OrderEvent orderEvent) {
        String lockKey = "order:lock:" + orderEvent.getOrderId();
        RLock lock = redissonClient.getLock(lockKey);
        boolean isLockAcquired = false;

        try {
            isLockAcquired = lock.tryLock(10, TimeUnit.SECONDS);
            if (!isLockAcquired) {
                log.warn("Unable to acquire lock for order: {}. Order cancellation aborted.", orderEvent.getOrderId());
                return;
            }

            log.info("Processing cancel order for user: {}. OrderId: {}", orderEvent.getUserId(), orderEvent.getOrderId());
            handleCancelOrder(orderEvent);
        } catch (Exception e) {
            log.error("Unexpected error processing cancel order for user: {}. OrderId: {}. Reason: {}", orderEvent.getUserId(), orderEvent.getOrderId(), e.getMessage());
            handleGenericException(orderEvent, e);
        } finally {
            if (isLockAcquired) {
                lock.unlock();
                log.info("Lock released for order: {}", orderEvent.getOrderId());
            }
        }
    }

    /**
     * Processes a buy order, ensuring stock availability, updating stock quantities, and notifying the user.
     * <p>
     * This method handles the main logic for processing a buy order. It checks if the order has been cancelled,
     * validates stock availability, updates stock quantities, and sends a notification to the user upon successful
     * order completion.
     * </p>
     *
     * @param orderEvent the order event to be processed.
     */
    private void handleBuyOrder(OrderEvent orderEvent) {
        log.debug("Initiating buy order handling for order: {} and stock: {}", orderEvent.getOrderId(), orderEvent.getTicker());

        if (isOrderCancelled(orderEvent.getOrderId())) {
            manageCancelledOrder(orderEvent);
            return;
        }

        Integer currentStock = getCachedStock(orderEvent.getTicker());
        validateStockAvailability(currentStock, orderEvent.getQuantity());

        StockEntity stock = getStockEntity(orderEvent.getTicker());
        validateStockQuantity(stock, orderEvent.getQuantity());

        continueBuyOrderProcess(orderEvent, stock);
    }

    private void continueBuyOrderProcess(OrderEvent orderEvent, StockEntity stock) {
        adjustStockQuantityAfterBuy(stock, orderEvent.getQuantity());
        adjustUserStockQuantityAfterBuy(orderEvent);
        persistOrder(orderEvent, OrderStatus.COMPLETED);
        persistOrderHistory(orderEvent, Optional.empty());
        updateOrderStatusInCache(orderEvent, OrderStatus.COMPLETED);

        notifyService.notifyUser(orderEvent.getUserId(), "Order successful. " + orderEvent.getQuantity() + " stocks bought.");
    }


    /**
     * Processes a sell order by validating stock availability, updating stock quantities, and notifying the user.
     * <p>
     * This method first checks if the order has been cancelled in the cache. If so, it manages the cancelled order.
     * Otherwise, it validates the user's stock availability and the system's stock after the sell operation.
     * It then updates the stock quantities, saves the order and its history, and finally notifies the user of the successful order.
     * </p>
     *
     * @param orderEvent The sell order event to be processed.
     */
    private void handleSellOrder(OrderEvent orderEvent) {
        log.debug("Handling sell order for stock: {}", orderEvent.getTicker());


        if (isOrderCancelled(orderEvent.getOrderId())) {
            log.warn("Order with ID {} was already cancelled.", orderEvent.getOrderId());
            manageCancelledOrder(orderEvent);
            return;
        }

        ensureUserHasSufficientStocks(orderEvent.getUserId(), orderEvent.getTicker(), orderEvent.getQuantity());
        ensureSystemHasSufficientStocksAfterSell(orderEvent.getTicker(), orderEvent.getQuantity());

        StockEntity stock = getStockEntity(orderEvent.getTicker());

        adjustStockQuantityAfterSell(stock, orderEvent.getQuantity());
        adjustUserStockQuantityAfterSell(orderEvent);
        persistOrder(orderEvent, OrderStatus.COMPLETED);
        persistOrderHistory(orderEvent, Optional.empty() );
        updateOrderStatusInCache(orderEvent, OrderStatus.COMPLETED);

        notifyService.notifyUser(orderEvent.getUserId(), "Order successful. " + orderEvent.getQuantity() + " stocks sold.");
    }

    /**
     * Processes a cancel order request.
     * <p>
     * This method checks if the order has already been completed. If the order has been processed,
     * it logs the order history as completed and notifies the user that the order cannot be cancelled.
     * </p>
     *
     * @param orderEvent The order event that the user wishes to cancel.
     */
    private void handleCancelOrder(OrderEvent orderEvent) {
        log.debug("Handling cancel order for stock: {}", orderEvent.getTicker());

        if (isOrderNotCancelled(orderEvent.getOrderId())) {
            persistOrderHistory(orderEvent, Optional.empty());
            notifyService.notifyUser(orderEvent.getUserId(), "Your order with ID " + orderEvent.getOrderId() + " has already been processed and cannot be cancelled.");
        }
    }

    /**
     * Checks if a given order has been marked as cancelled in Redis.
     * <p>
     * This method queries the Redis cache to determine if a specific order has been cancelled by the user.
     * It provides a quick way to verify the status of an order before processing or taking any other action on it.
     * </p>
     *
     * @param orderId The unique identifier of the order to be checked.
     * @return true if the order has been cancelled, false otherwise.
     * @see RedisTemplate#opsForSet()
     */
    private boolean isOrderCancelled(String orderId) {
        return orderRedisService.isCancelledOrderInCache(orderId);
    }

    private boolean isOrderNotCancelled(String orderId) {
        return !isOrderCancelled(orderId);
    }


    /**
     * Updates the stock quantity in the database and cache after a sell order.
     *
     * @param stock         the stock entity.
     * @param orderQuantity the quantity of the order.
     */
    private void adjustStockQuantityAfterSell(StockEntity stock, int orderQuantity) {
        log.debug("Updating stock after sell for: {}", stock.getTicker());
        stock.setQuantity(stock.getQuantity() + orderQuantity);
        stockRepository.save(stock);
        stockCacheService.updateStockInCache(stock.getTicker(), stock.getQuantity());
    }

    /**
     * Updates the user's stock quantity in the database after a buy operation.
     * <p>
     * This method checks if the user already has a record for the given stock.
     * If the user has a record, it updates the quantity by adding the new quantity.
     * If the user doesn't have a record, it creates a new one with the given quantity.
     * </p>
     *
     * @param orderEvent The order event containing details of the buy operation.
     */
    private void adjustUserStockQuantityAfterBuy(OrderEvent orderEvent) {
        // Fetch the user's stock record for the given stock name
        UserStockEntity userStock = userStockRepository.findByUserIdAndTicker(orderEvent.getUserId(), orderEvent.getTicker());

        // If no record found, create a new one
        if (userStock == null) {
            userStock = new UserStockEntity();
            userStock.setQuantity(orderEvent.getQuantity());
            log.info("Creating a new stock record for user ID: {} and stock name: {}", orderEvent.getUserId(), orderEvent.getTicker());
        } else {
            // If a record is found, update the quantity
            userStock.setQuantity(userStock.getQuantity() + orderEvent.getQuantity());
            log.info("Updating stock quantity for user ID: {} and stock name: {}. New quantity: {}", orderEvent.getUserId(), orderEvent.getTicker(), userStock.getQuantity());
        }

        // Set the stock name and user ID
        userStock.setTicker(orderEvent.getTicker());
        userStock.setUserId(orderEvent.getUserId());

        // Save the updated or new record to the database
        userStockRepository.save(userStock);
        log.info("Successfully saved stock record for user ID: {} and stock name: {}", orderEvent.getUserId(), orderEvent.getTicker());
    }

    /**
     * Updates the user's stock quantity in the database after a sell operation.
     * <p>
     * This method assumes that the user already has a record for the given stock.
     * It updates the quantity by subtracting the sold quantity.
     * </p>
     *
     * @param orderEvent The order event containing details of the sell operation.
     */
    private void adjustUserStockQuantityAfterSell(OrderEvent orderEvent) {
        // Fetch the user's stock record for the given stock name
        UserStockEntity userStock = userStockRepository.findByUserIdAndTicker(orderEvent.getUserId(), orderEvent.getTicker());

        // Subtract the sold quantity from the existing quantity
        int newQuantity = userStock.getQuantity() - orderEvent.getQuantity();

        // Ensure that the new quantity is not negative
        if (newQuantity < 0) {
            log.error("Error: Selling quantity exceeds the available stock for user ID: {} and stock name: {}", orderEvent.getUserId(), orderEvent.getTicker());
            throw new RuntimeException("Selling quantity exceeds the available stock.");
        }

        userStock.setQuantity(newQuantity);
        log.info("Updating stock quantity after sell for user ID: {} and stock name: {}. New quantity: {}", orderEvent.getUserId(), orderEvent.getTicker(), newQuantity);

        // Save the updated record to the database
        userStockRepository.save(userStock);
        log.info("Successfully updated stock record after sell for user ID: {} and stock name: {}", orderEvent.getUserId(), orderEvent.getTicker());
    }


    /**
     * Validates if the user has enough stocks to sell.
     *
     * @param userId    the user id.
     * @param stockName the name of the stock.
     * @param quantity  the quantity to be sold.
     */
    private void ensureUserHasSufficientStocks(Long userId, String stockName, int quantity) {
        UserStockEntity userStock = userStockRepository.findByUserIdAndTicker(userId, stockName);

        if (userStock == null) {
            throw new InsufficientStockException("User does not own any stocks of " + stockName);
        }

        if (userStock.getQuantity() < quantity) {
            throw new InsufficientStockException("User does not have enough stocks of " + stockName + " to sell. Owned: " + userStock.getQuantity() + ", Requested to sell: " + quantity);
        }
    }

    /**
     * Validates if the system's stock limit will be exceeded after the sell operation.
     * <p>
     * This method checks the current stock in the system (from cache) and validates
     * whether selling the given quantity will exceed the system's predefined stock limit
     * (in this case, 10 for APPLE stocks). If the limit is exceeded, an exception is thrown.
     * </p>
     *
     * @param ticker the name of the stock being sold.
     * @param quantity  the quantity of the stock to be sold.
     * @throws ExceedingSystemStockLimitException if selling the given quantity will exceed the system's stock limit.
     */
    private void ensureSystemHasSufficientStocksAfterSell(String ticker, int quantity) {
        Integer currentSystemStock = getCachedStock(ticker);
        if (currentSystemStock + quantity > systemStockLimit) {
            throw new ExceedingSystemStockLimitException("Selling this quantity will exceed the system's stock limit for " + ticker);
        }
    }


    /**
     * Retrieves the cached stock quantity for a given stock name.
     *
     * @param ticker the name of the stock.
     * @return the cached stock quantity.
     */
    private Integer getCachedStock(String ticker) {
        log.debug("Fetching cached stock for: {}", ticker);
        return stockCacheService.getCachedStock(ticker);
    }

    private void validateStockAvailability(Integer currentStock, int orderQuantity) {
        log.debug("Validating stock availability for order quantity: {}", orderQuantity);
        if (currentStock == null || currentStock < orderQuantity) {
            throw new InsufficientStockException(INSUFFICIENT_STOCK_MESSAGE);
        }
    }

    private StockEntity getStockEntity(String ticker) {
        log.debug("Fetching stock entity for: {}", ticker);
        StockEntity stock = stockRepository.findByTicker(ticker);
        if (stock == null) {
            throw new StockNotFoundException(STOCK_NOT_FOUND_MESSAGE + ticker);
        }
        return stock;
    }

    private void validateStockQuantity(StockEntity stock, int orderQuantity) {
        log.debug("Validating stock quantity for order quantity: {}", orderQuantity);
        if (stock.getQuantity() < orderQuantity) {
            throw new InsufficientStockException(INSUFFICIENT_STOCK_MESSAGE);
        }
    }

    /**
     * Updates the stock quantity in the database and cache.
     *
     * @param stock         the stock entity.
     * @param orderQuantity the quantity of the order.
     */
    private void adjustStockQuantityAfterBuy(StockEntity stock, int orderQuantity) {
        log.debug("Updating stock for: {}", stock.getTicker());
        stock.setQuantity(stock.getQuantity() - orderQuantity);
        stockRepository.save(stock);
        stockCacheService.updateStockInCache(stock.getTicker(), stock.getQuantity());
    }

    /**
     * Handles exceptions related to stock availability and stock not found.
     *
     * @param orderEvent the order event.
     * @param e          the exception.
     */
    private void handleOrderException(OrderEvent orderEvent, Exception e) {
        log.warn(e.getMessage() + " for user: {}. OrderId: {}", orderEvent.getUserId(), orderEvent.getOrderId());
        notifyService.notifyUser(orderEvent.getUserId(), e.getMessage());
        persistOrder(orderEvent, OrderStatus.FAILED);
        persistOrderHistory(orderEvent, Optional.of(e.getMessage()));
        updateOrderStatusInCache(orderEvent, OrderStatus.FAILED);
    }

    private void handleGenericException(OrderEvent orderEvent, Exception e) {
        log.error("An error occurred while processing the order for user: {}. OrderId: {}", orderEvent.getUserId(), orderEvent.getOrderId(), e);
        notifyService.notifyUser(orderEvent.getUserId(), "Order failed due to a system error.");
        persistOrder(orderEvent, OrderStatus.FAILED);
        persistOrderHistory(orderEvent, Optional.of(e.getMessage()));
        updateOrderStatusInCache(orderEvent, OrderStatus.FAILED);
    }


    private void manageCancelledOrder(OrderEvent orderEvent) {
        log.warn("Order with ID {} was already cancelled.", orderEvent.getOrderId());
        persistOrder(orderEvent, OrderStatus.CANCELLED);
        persistOrderHistory(orderEvent, Optional.empty());
        updateOrderStatusInCache(orderEvent, OrderStatus.CANCELLED);
        notifyService.notifyUser(orderEvent.getUserId(), "Your order with ID " + orderEvent.getOrderId() + " has been successfully cancelled.");
    }

    private void persistOrder(OrderEvent orderEvent, OrderStatus orderStatus) {
        orderService.saveOrder(orderEvent, orderStatus);
    }

    private void updateOrderStatusInCache(OrderEvent orderEvent, OrderStatus orderStatus) {
        orderRedisService.updateOrderStatusInCache(orderEvent.getOrderId(), orderStatus);
    }
    private void persistOrderHistory(OrderEvent orderEvent, Optional<String> errorMessage) {
        log.debug("Saving order history for user: {}. OrderId: {}", orderEvent.getUserId(), orderEvent.getOrderId());
        orderHistoryService.saveOrderHistory(orderEvent, errorMessage);
    }

}
