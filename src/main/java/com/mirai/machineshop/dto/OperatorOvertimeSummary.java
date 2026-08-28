package com.mirai.machineshop.dto;

import java.time.LocalDate;

public record OperatorOvertimeSummary(
        Long operatorId,
        String operatorCode,
        String operatorName,
        LocalDate workDate,
        int scheduledMinutes,
        int regularMinutes,
        int overtimeMinutes,
        double overtimeCost
) {
}
