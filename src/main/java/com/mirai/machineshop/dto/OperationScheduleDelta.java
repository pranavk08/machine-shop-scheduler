package com.mirai.machineshop.dto;

import java.time.LocalDateTime;

public record OperationScheduleDelta(
        String orderNumber,
        Integer sequenceNumber,
        String operationType,
        String beforeMachineCode,
        String afterMachineCode,
        String beforeOperatorName,
        String afterOperatorName,
        LocalDateTime beforeStartTime,
        LocalDateTime afterStartTime,
        LocalDateTime beforeEndTime,
        LocalDateTime afterEndTime,
        long delayMinutes,
        boolean machineChanged,
        boolean operatorChanged,
        boolean timeChanged) {
}
