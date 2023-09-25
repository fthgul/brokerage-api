package com.midas.studycase.brokerageapi.service.impl;

import com.midas.studycase.brokerageapi.exception.OrderNotFoundException;
import com.midas.studycase.brokerageapi.model.entity.OrderEntity;
import com.midas.studycase.brokerageapi.model.enums.OrderStatus;
import com.midas.studycase.brokerageapi.model.event.OrderEvent;
import com.midas.studycase.brokerageapi.model.mapper.OrderDetailMapper;
import com.midas.studycase.brokerageapi.model.response.OrderDetailResponse;
import com.midas.studycase.brokerageapi.repository.OrderEntityRepository;
import com.midas.studycase.brokerageapi.service.OrderService;
import com.midas.studycase.brokerageapi.service.cache.OrderRedisReactiveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRedisReactiveService orderRedisReactiveService;
    private final OrderEntityRepository orderRepository;
    private final OrderDetailMapper orderDetailMapper;


    /**
     * Fetches the details of an order by its ID.
     * First, it tries to get the order details from the cache.
     * If not found in the cache, it fetches the details from the database.
     *
     * @param orderId The ID of the order to be fetched.
     * @return A Mono emitting the details of the order as an OrderDetailResponse.
     *         If the order is not found, a Mono error is emitted with a meaningful message.
     */
    @Override
    public Mono<OrderDetailResponse> getOrderDetails(String orderId) {
        return orderRedisReactiveService.getOrderFromCache(orderId)
                .switchIfEmpty(
                        Mono.defer(() -> Mono.fromCallable(() -> orderRepository.findByOrderIdWithHistories(orderId))
                                .subscribeOn(Schedulers.boundedElastic())
                                .flatMap(orderEntityOpt -> {
                                    if (orderEntityOpt.isPresent()) {
                                        OrderDetailResponse response = orderDetailMapper.toOrderDetailResponse(orderEntityOpt.get());
                                        return Mono.just(response);
                                    } else {
                                        return Mono.error(new OrderNotFoundException("Order not found with ID: " + orderId));
                                    }
                                }))
                )
                .onErrorResume(OrderNotFoundException.class, Mono::error)
                .onErrorResume(e -> {
                    if (!(e instanceof OrderNotFoundException)) {
                        String errorMsg = "An error occurred while fetching order details for ID: " + orderId;
                        log.error(errorMsg, e);
                        return Mono.error(new RuntimeException(errorMsg, e));
                    }
                    return Mono.error(e);
                });
    }



    @Override
    public Flux<OrderDetailResponse> getUserOrders(Long userId, Integer page, Integer size) {
        int currentPage = page != null ? page : 0;
        int pageSize = size != null ? size : 10;

        Pageable pageable = PageRequest.of(currentPage, pageSize, Sort.by(Sort.Order.desc("createdAt")));
        return orderRedisReactiveService.getLastOrdersForUser(userId, currentPage, pageSize)
                .switchIfEmpty(
                        Flux.defer(() -> Mono.fromCallable(() -> orderRepository.findOrdersForUser(userId, pageable))
                                .subscribeOn(Schedulers.boundedElastic())
                                .flatMapMany(ordersPage -> {
                                    List<OrderDetailResponse> responses = ordersPage.getContent().stream()
                                            .map(orderDetailMapper::toOrderDetailResponse)
                                            .collect(Collectors.toList());
                                    return Flux.fromIterable(responses);
                                }))
                )
                .onErrorResume(e -> {
                    String errorMsg = "An error occurred while fetching orders for user ID: " + userId;
                    log.error(errorMsg, e);
                    return Flux.error(new RuntimeException(errorMsg, e));
                });
    }


    @Override
    @Transactional
    public void saveOrder(OrderEvent orderEvent, OrderStatus orderStatus) {
        orderRepository.save(prepareOrderEntity(orderEvent, orderStatus));
    }


    private OrderEntity prepareOrderEntity(OrderEvent orderEvent, OrderStatus orderStatus) {
        OrderEntity order = new OrderEntity();
        order.setOrderId(orderEvent.getOrderId());
        order.setUserId(orderEvent.getUserId());
        order.setTicker(orderEvent.getTicker());
        order.setQuantity(orderEvent.getQuantity());
        order.setOrderType(orderEvent.getOrderType());
        order.setStatus(orderStatus);
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        return order;
    }
}
