package com.midas.studycase.brokerageapi.model.request;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class BuyOrderRequest extends OrderRequest {

    private String orderId;

    @Min(value = 1, message = "quantity must be greater than 0")
    private int quantity;

}
