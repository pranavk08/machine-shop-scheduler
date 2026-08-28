package com.mirai.machineshop.dto;

import java.time.LocalDateTime;
import java.util.List;

public record StrategyComparisonResponse(
        LocalDateTime timestamp,
        List<StrategyEvaluationResult> strategies,
        String recommendedStrategy,
        String recommendationReason
) {
}
