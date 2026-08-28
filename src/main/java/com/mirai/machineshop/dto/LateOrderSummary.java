package com.mirai.machineshop.dto;

import java.time.LocalDateTime;

public record LateOrderSummary(
        Long orderId,
        String orderNumber,
        String customerName,
        String customerTier,
        LocalDateTime dueDate,
        LocalDateTime completionDate,
        double delayHours,
        double penaltyRatePerHour,
        double penaltyAmount
) {
}
