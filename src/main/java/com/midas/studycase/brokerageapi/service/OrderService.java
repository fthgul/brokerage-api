package com.midas.studycase.brokerageapi.service;

import com.midas.studycase.brokerageapi.model.enums.OrderStatus;
import com.midas.studycase.brokerageapi.model.event.OrderEvent;
import com.midas.studycase.brokerageapi.model.response.OrderDetailResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface OrderService {
    Mono<OrderDetailResponse> getOrderDetails(String orderId);
    Flux<OrderDetailResponse> getUserOrders(Long userId, Integer page, Integer size);
    void saveOrder(OrderEvent orderEvent, OrderStatus orderStatus);
}
