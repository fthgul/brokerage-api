package com.midas.studycase.brokerageapi.controller;

import com.midas.studycase.brokerageapi.model.request.BuyOrderRequest;
import com.midas.studycase.brokerageapi.model.request.CancelOrderRequest;
import com.midas.studycase.brokerageapi.model.request.SellOrderRequest;
import com.midas.studycase.brokerageapi.model.response.OrderResponse;
import com.midas.studycase.brokerageapi.service.TradeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/trades")
@RequiredArgsConstructor
public class TradeController {

    private final TradeService tradeService;

    @PostMapping("/buy")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Mono<OrderResponse> buyOrder(@Valid @RequestBody BuyOrderRequest order) {
        return tradeService.processBuyOrder(order);
    }

    @PostMapping("/sell")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Mono<OrderResponse> sellOrder(@Valid @RequestBody SellOrderRequest order) {
        return tradeService.processSellOrder(order);
    }

    @PostMapping("/cancel")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Mono<OrderResponse> cancelOrder(@Valid @RequestBody CancelOrderRequest order) {
        return tradeService.processCancelOrder(order);
    }

}
