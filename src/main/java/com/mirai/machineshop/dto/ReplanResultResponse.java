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
        List<OperationScheduleDelta> impactDeltas,
        CostImpactSummary beforeCostSummary,
        CostImpactSummary afterCostSummary,
        double netCostImpact,
        OvertimeDecisionComparison overtimeComparison,
        List<OvertimeDecisionComparison> overtimeComparisons) {

    public ReplanResultResponse(
            LocalDateTime replanTimestamp,
            int totalOperations,
            int operationsMovedCount,
            int ordersDelayedCount,
            int machinesReassignedCount,
            int operatorsReassignedCount,
            List<ScheduleResult> beforeSchedule,
            List<ScheduleResult> afterSchedule,
            List<OperationScheduleDelta> impactDeltas,
            CostImpactSummary beforeCostSummary,
            CostImpactSummary afterCostSummary,
            double netCostImpact,
            OvertimeDecisionComparison overtimeComparison) {
        this(replanTimestamp, totalOperations, operationsMovedCount, ordersDelayedCount,
                machinesReassignedCount, operatorsReassignedCount, beforeSchedule, afterSchedule,
                impactDeltas, beforeCostSummary, afterCostSummary, netCostImpact,
                overtimeComparison,
                overtimeComparison != null ? List.of(overtimeComparison) : List.of());
    }

    public ReplanResultResponse(
            LocalDateTime replanTimestamp,
            int totalOperations,
            int operationsMovedCount,
            int ordersDelayedCount,
            int machinesReassignedCount,
            int operatorsReassignedCount,
            List<ScheduleResult> beforeSchedule,
            List<ScheduleResult> afterSchedule,
            List<OperationScheduleDelta> impactDeltas,
            CostImpactSummary beforeCostSummary,
            CostImpactSummary afterCostSummary,
            double netCostImpact) {
        this(replanTimestamp, totalOperations, operationsMovedCount, ordersDelayedCount,
                machinesReassignedCount, operatorsReassignedCount, beforeSchedule, afterSchedule,
                impactDeltas, beforeCostSummary, afterCostSummary, netCostImpact,
                OvertimeDecisionComparison.noDecisionRequired(), List.of());
    }

    public ReplanResultResponse(
            LocalDateTime replanTimestamp,
            int totalOperations,
            int operationsMovedCount,
            int ordersDelayedCount,
            int machinesReassignedCount,
            int operatorsReassignedCount,
            List<ScheduleResult> beforeSchedule,
            List<ScheduleResult> afterSchedule,
            List<OperationScheduleDelta> impactDeltas) {
        this(replanTimestamp, totalOperations, operationsMovedCount, ordersDelayedCount,
                machinesReassignedCount, operatorsReassignedCount, beforeSchedule, afterSchedule,
                impactDeltas, null, null, 0.0,
                OvertimeDecisionComparison.noDecisionRequired(), List.of());
    }
}
