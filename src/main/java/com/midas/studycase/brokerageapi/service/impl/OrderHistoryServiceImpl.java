package com.midas.studycase.brokerageapi.service.impl;

import com.midas.studycase.brokerageapi.model.entity.OrderHistoryEntity;
import com.midas.studycase.brokerageapi.model.event.OrderEvent;
import com.midas.studycase.brokerageapi.repository.OrderHistoryRepository;
import com.midas.studycase.brokerageapi.service.OrderHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderHistoryServiceImpl implements OrderHistoryService {

    private final OrderHistoryRepository orderHistoryRepository;

    @Override
    @Transactional
    public void saveOrderHistory(OrderEvent orderEvent, Optional<String> reason) {
        final OrderHistoryEntity orderHistoryEntity = new OrderHistoryEntity();
        orderHistoryEntity.setTransactionId(UUID.randomUUID().toString());
        orderHistoryEntity.setOrderId(orderEvent.getOrderId());
        orderHistoryEntity.setUserId(orderEvent.getUserId());
        orderHistoryEntity.setTicker(orderEvent.getTicker());
        orderHistoryEntity.setOrderType(orderEvent.getOrderType());
        orderHistoryEntity.setQuantity(orderEvent.getQuantity());
        orderHistoryEntity.setCreatedAt(LocalDateTime.now());
        orderHistoryEntity.setUpdatedAt(LocalDateTime.now());
        reason.ifPresent(orderHistoryEntity::setReason);

        orderHistoryRepository.save(orderHistoryEntity);
    }
}
