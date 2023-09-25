package com.midas.studycase.brokerageapi.model.entity;


import com.midas.studycase.brokerageapi.model.enums.OrderType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "order_history")
public class OrderHistoryEntity {

    @Id
    @Column(name = "transaction_id")
    private String transactionId;

    @Column(name = "order_id")
    private String orderId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "ticker")
    private String ticker;

    @Column(name = "order_type")
    @Enumerated(value = EnumType.STRING)
    private OrderType orderType;

    @Column(name = "quantity")
    private Integer quantity;

    private String reason;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

