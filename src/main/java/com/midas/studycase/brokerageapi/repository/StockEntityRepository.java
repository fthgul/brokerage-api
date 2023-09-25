package com.midas.studycase.brokerageapi.repository;

import com.midas.studycase.brokerageapi.model.entity.StockEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface StockEntityRepository extends CrudRepository<StockEntity, Long> {
    StockEntity findByTicker(String ticker);
}
