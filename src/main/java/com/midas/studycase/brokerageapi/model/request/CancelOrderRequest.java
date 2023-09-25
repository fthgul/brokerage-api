package com.midas.studycase.brokerageapi.model.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CancelOrderRequest extends OrderRequest {
    @NotNull(message = "orderId cannot be null")
    private String orderId;
}
