package com.midas.studycase.brokerageapi.service.cache;

import com.midas.studycase.brokerageapi.model.enums.OrderStatus;
import com.midas.studycase.brokerageapi.model.event.OrderEvent;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class BaseOrderService {
    protected static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    public String generateOrderKey(String orderId) {
        return "order:" + orderId;
    }

    protected String getOrderKey(String orderId) {
        return "order:" + orderId;
    }

    /**
     * Creates a map representation of the given OrderEvent and order status.
     * This map is used for caching the order details in Redis.
     *
     * @param orderEvent  The OrderEvent to convert.
     * @param orderStatus The status of the order.
     * @return A map representation of the OrderEvent and order status.
     */
    protected Map<String, Object> createOrderMap(OrderEvent orderEvent, OrderStatus orderStatus) {
        Map<String, Object> orderMap = new HashMap<>();
        orderMap.put("currentStatus", orderStatus);
        orderMap.put("orderId", orderEvent.getOrderId());
        orderMap.put("ticker", orderEvent.getTicker());
        orderMap.put("quantity", orderEvent.getQuantity());
        orderMap.put("userId", orderEvent.getUserId());
        orderMap.put("createdAt", orderEvent.getCreatedAt().toString());
        orderMap.put("updatedAt", orderEvent.getCreatedAt().toString());

        // Assuming you have a method to fetch the existing history or it's passed in the orderEvent
        List<Map<String, String>> history = new ArrayList<>();
        Map<String, String> historyEntry = new HashMap<>();
        historyEntry.put("orderType", orderEvent.getOrderType().toString());
        historyEntry.put("timestamp", orderEvent.getCreatedAt().toString());
        history.add(historyEntry);

        orderMap.put("history", history);

        return orderMap;
    }

    protected String getStringValue(Map<String, Object> map, String key) {
        return map.containsKey(key) ? String.valueOf(map.get(key)) : null;
    }

    protected LocalDateTime getDateValue(Map<String, Object> map, String key) {
        String dateTimeStr = (String) map.getOrDefault(key, null);
        if (dateTimeStr == null) {
            return null;
        }

        return LocalDateTime.parse(dateTimeStr, FORMATTER);
    }

    protected Long getLongValue(Map<String, Object> map, String key) {
        return map.containsKey(key) ? Long.parseLong(String.valueOf(map.get(key))) : null;
    }

    protected Integer getIntValue(Map<String, Object> map, String key) {
        return map.containsKey(key) ? Integer.parseInt(String.valueOf(map.get(key))) : null;
    }

    protected <T extends Enum<T>> T getEnumValue(Map<String, Object> map, String key, Class<T> enumType) {
        return map.containsKey(key) ? Enum.valueOf(enumType, String.valueOf(map.get(key))) : null;
    }

}
