package com.mirai.machineshop.dto;

import java.util.List;

public record CostImpactSummary(
        double totalOvertimeHours,
        double totalOvertimeCost,
        int lateOrdersCount,
        double totalPenaltyCost,
        double totalWastedChangeoverCost,
        double totalCost,
        List<OperatorOvertimeSummary> operatorOvertimes,
        List<LateOrderSummary> lateOrders
) {
}
