package com.midas.studycase.brokerageapi.exception;

public class ExceedingSystemStockLimitException extends RuntimeException {
    public ExceedingSystemStockLimitException(String message) {
        super(message);
    }
}
