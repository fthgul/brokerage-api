package com.midas.studycase.brokerageapi.repository;

import com.midas.studycase.brokerageapi.model.entity.UserStockEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserStockEntityRepository extends CrudRepository<UserStockEntity, Long> {
    UserStockEntity findByUserIdAndTicker(Long userId, String ticker);
}
