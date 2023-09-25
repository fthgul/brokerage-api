package com.midas.studycase.brokerageapi.service.cache;

import com.midas.studycase.brokerageapi.exception.StockNotFoundException;
import com.midas.studycase.brokerageapi.model.entity.StockEntity;
import com.midas.studycase.brokerageapi.repository.StockEntityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;


@Slf4j
@Service
@RequiredArgsConstructor
public class StockCacheService {
    private static final String STOCK_SUFFIX = "_STOCK";
    private static final String STOCK_NOT_FOUND_MESSAGE = "Stock not found for stockName: ";

    private final StockEntityRepository stockRepository;

    @Cacheable(value = "stocks", key = "#stockName.concat('" + STOCK_SUFFIX + "')")
    public Integer getCachedStock(String stockName) {
        return getStockEntity(stockName).getQuantity();
    }
    @CachePut(value = "stocks", key = "#stockName.concat('" + STOCK_SUFFIX + "')")
    public Integer updateStockInCache(String stockName, Integer newStockValue) {
        return newStockValue;
    }

    @CacheEvict(value = "stocks", key = "#stockName.concat('" + STOCK_SUFFIX + "')")
    public void evictStockFromCache(String stockName) {
    }



    private StockEntity getStockEntity(String stockName) {
        StockEntity stock = stockRepository.findByTicker(stockName);
        if (stock == null) {
            throw new StockNotFoundException(STOCK_NOT_FOUND_MESSAGE + stockName);
        }
        return stock;
    }
}

