package com.midas.studycase.brokerageapi.model.enums;
public enum OrderStatus {
    CREATED,     // The order has been successfully registered in the system but not yet processed.
    CANCELLED,   // The order has been cancelled by the user or due to some system conditions.
    COMPLETED,   // The order has been successfully processed and executed in the market.
    FAILED;      // The order could not be processed due to business rule violations or other issues.
}


