package com.midas.studycase.brokerageapi.controller;

import com.midas.studycase.brokerageapi.model.response.OrderDetailResponse;
import com.midas.studycase.brokerageapi.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    @GetMapping("/{orderId}")
    public Mono<OrderDetailResponse> getOrderDetails(@PathVariable String orderId) {
        return orderService.getOrderDetails(orderId);
    }

    @GetMapping("/user/{userId}")
    public Flux<OrderDetailResponse> getUserOrders(
            @PathVariable Long userId,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        return orderService.getUserOrders(userId, page, pageSize);
    }

}
