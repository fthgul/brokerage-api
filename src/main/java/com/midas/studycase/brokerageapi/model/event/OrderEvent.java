package com.midas.studycase.brokerageapi.model.event;

import com.midas.studycase.brokerageapi.model.enums.OrderType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;



@Getter
@Setter
@NoArgsConstructor
@ToString
public class OrderEvent {
    private String orderId;
    private String transactionId;
    private long userId;
    private OrderType orderType;
    private String ticker;
    private int quantity;
    private LocalDateTime createdAt;
}

