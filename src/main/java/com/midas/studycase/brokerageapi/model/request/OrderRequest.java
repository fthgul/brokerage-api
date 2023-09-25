package com.midas.studycase.brokerageapi.model.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class OrderRequest {

    @NotNull(message = "userId cannot be null")
    private Long userId;

    @NotNull(message = "ticker cannot be null")
    private String ticker;

}
