package com.midas.studycase.brokerageapi.model.mapper;

import com.midas.studycase.brokerageapi.model.entity.OrderEntity;
import com.midas.studycase.brokerageapi.model.entity.OrderHistoryEntity;
import com.midas.studycase.brokerageapi.model.response.OrderDetailResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface OrderDetailMapper {

    @Mapping(source = "orderHistories", target = "orderHistories")
    OrderDetailResponse toOrderDetailResponse(OrderEntity orderEntity);

    List<OrderDetailResponse.OrderHistory> toOrderHistoryList(List<OrderHistoryEntity> orderHistoryEntities);

    @Mapping(source = "orderType", target = "orderType")
    @Mapping(source = "createdAt", target = "createdAt")
    OrderDetailResponse.OrderHistory toOrderHistory(OrderHistoryEntity orderHistoryEntity);


}
