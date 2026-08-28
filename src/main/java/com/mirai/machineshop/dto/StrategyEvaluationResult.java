package com.mirai.machineshop.dto;

public record StrategyEvaluationResult(
        String strategy,
        String displayName,
        String description,
        int totalOperations,
        double totalScheduleDurationHours,
        CostImpactSummary costSummary
) {
}
