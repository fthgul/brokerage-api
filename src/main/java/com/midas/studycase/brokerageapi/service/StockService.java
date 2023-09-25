package com.midas.studycase.brokerageapi.service;

import com.midas.studycase.brokerageapi.model.event.OrderEvent;


public interface StockService {
    void processBuyOrder(OrderEvent buyOrderEvent);
    void processSellOrder(OrderEvent sellOrderEvent);
    void processCancelOrder(OrderEvent cancelOrderEvent);
}
