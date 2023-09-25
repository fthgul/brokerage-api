package com.midas.studycase.brokerageapi.repository;

import com.midas.studycase.brokerageapi.model.entity.OrderHistoryEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderHistoryRepository extends CrudRepository<OrderHistoryEntity, Long> {
    Optional<OrderHistoryEntity> findByOrderId(String orderId);
}
