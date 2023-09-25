package com.midas.studycase.brokerageapi.service;

import com.midas.studycase.brokerageapi.model.event.OrderEvent;

import java.util.Optional;

public interface OrderHistoryService {
    void saveOrderHistory(OrderEvent orderEvent, Optional<String> reason);
}
