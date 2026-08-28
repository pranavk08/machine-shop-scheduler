package com.mirai.machineshop.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.mirai.machineshop.scheduler.ScheduleResult;

public record ReplanResultResponse(
        LocalDateTime replanTimestamp,
        int totalOperations,
        int operationsMovedCount,
        int ordersDelayedCount,
        int machinesReassignedCount,
        int operatorsReassignedCount,
        List<ScheduleResult> beforeSchedule,
        List<ScheduleResult> afterSchedule,
        List<OperationScheduleDelta> impactDeltas) {
}
