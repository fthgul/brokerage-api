package com.midas.studycase.brokerageapi.model.response;

import com.midas.studycase.brokerageapi.model.enums.OrderStatus;
import com.midas.studycase.brokerageapi.model.enums.OrderType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class OrderDetailResponse {
    private String orderId;
    private String userId;
    private OrderStatus status;
    private String ticker;
    private Integer quantity;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<OrderHistory> orderHistories;

    @Getter
    @Setter
    public static class OrderHistory {
        private OrderType orderType;
        private LocalDateTime createdAt;
    }
}

