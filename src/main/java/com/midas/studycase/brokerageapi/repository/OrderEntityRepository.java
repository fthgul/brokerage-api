package com.midas.studycase.brokerageapi.repository;

import com.midas.studycase.brokerageapi.model.entity.OrderEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderEntityRepository extends CrudRepository<OrderEntity, String>{

    @Query("SELECT o FROM OrderEntity o JOIN FETCH o.orderHistories WHERE o.orderId = :orderId")
    Optional<OrderEntity> findByOrderIdWithHistories(@Param("orderId") String orderId);

    @Query("SELECT o FROM OrderEntity o LEFT JOIN FETCH o.orderHistories WHERE o.userId = :userId")
    Page<OrderEntity> findOrdersForUser(@Param("userId") Long userId, Pageable pageable);
}

