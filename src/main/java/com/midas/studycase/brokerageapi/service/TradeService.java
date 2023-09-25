package com.midas.studycase.brokerageapi.service;

import com.midas.studycase.brokerageapi.model.request.BuyOrderRequest;
import com.midas.studycase.brokerageapi.model.request.CancelOrderRequest;
import com.midas.studycase.brokerageapi.model.request.SellOrderRequest;
import com.midas.studycase.brokerageapi.model.response.OrderResponse;
import reactor.core.publisher.Mono;

public interface TradeService {
    Mono<OrderResponse> processBuyOrder(BuyOrderRequest order);
    Mono<OrderResponse> processSellOrder(SellOrderRequest order);
    Mono<OrderResponse> processCancelOrder(CancelOrderRequest order);
}
