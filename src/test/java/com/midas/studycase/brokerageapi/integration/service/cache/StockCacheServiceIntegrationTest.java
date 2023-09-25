package com.midas.studycase.brokerageapi.integration.service.cache;


import com.midas.studycase.brokerageapi.config.redis.CacheConfig;
import com.midas.studycase.brokerageapi.exception.StockNotFoundException;
import com.midas.studycase.brokerageapi.integration.config.RedisContainerConfig;
import com.midas.studycase.brokerageapi.integration.config.TestConfigRedis;
import com.midas.studycase.brokerageapi.integration.config.property.RedisProperties;
import com.midas.studycase.brokerageapi.model.entity.StockEntity;
import com.midas.studycase.brokerageapi.repository.StockEntityRepository;
import com.midas.studycase.brokerageapi.service.cache.StockCacheService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@Testcontainers
@SpringBootTest(classes = {StockCacheService.class, RedisProperties.class, TestConfigRedis.class, CacheConfig.class, RedisContainerConfig.class})
@TestPropertySource(locations = "classpath:application-test.properties")
public class StockCacheServiceIntegrationTest {

    @Autowired
    private StockCacheService stockCacheService;

    @MockBean
    private StockEntityRepository stockRepository;

    @Autowired
    private CacheManager cacheManager;

    @Test
    @DisplayName("Should Retrieve Cached Stock and Validate Cache Entry")
    public void shouldRetrieveCachedStockAndValidateCacheEntry() {
        // Given
        String stockName = "testStock";
        StockEntity stockEntity = new StockEntity();
        stockEntity.setTicker(stockName);
        stockEntity.setQuantity(100);
        when(stockRepository.findByTicker(stockName)).thenReturn(stockEntity);

        // When
        Integer quantity = stockCacheService.getCachedStock(stockName);

        // Then
        assertEquals(100, quantity);
        assertNotNull(cacheManager.getCache("stocks").get(stockName + "_STOCK"));
    }

    @Test
    @DisplayName("Should Update Stock In Cache")
    public void shouldUpdateStockInCache() {
        // Given
        String stockName = "testStock2";
        Integer newStockValue = 200;

        // When
        stockCacheService.updateStockInCache(stockName, newStockValue);

        // Then
        assertEquals(newStockValue, cacheManager.getCache("stocks").get(stockName + "_STOCK").get());
    }

    @Test
    @DisplayName("Should Evict Stock From Cache")
    public void shouldEvictStockFromCache() {
        // Given
        String stockName = "testStock3";
        cacheManager.getCache("stocks").put(stockName + "_STOCK", 100);

        // When
        stockCacheService.evictStockFromCache(stockName);

        // Then
        assertNull(cacheManager.getCache("stocks").get(stockName + "_STOCK"));
    }

    @Test
    @DisplayName("Should Throw StockNotFoundException When Stock Entity Not Found")
    public void shouldThrowStockNotFoundExceptionWhenStockEntityNotFound() {
        // Given
        String stockName = "nonExistentStock";
        when(stockRepository.findByTicker(stockName)).thenReturn(null);

        // When & Then
        assertThrows(StockNotFoundException.class, () -> stockCacheService.getCachedStock(stockName));
    }
}

