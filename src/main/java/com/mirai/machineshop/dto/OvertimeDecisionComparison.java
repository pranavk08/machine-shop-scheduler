package com.mirai.machineshop.dto;

import java.time.LocalDateTime;

public record OvertimeDecisionComparison(
        String decisionStatus,              // "OVERTIME_SELECTED", "OVERTIME_REJECTED", "NO_DECISION_REQUIRED"
        String orderNumber,                 // e.g. "ORD-001"
        Integer sequenceNumber,             // e.g. 1
        String operationType,               // e.g. "TURNING"
        String customerName,                // e.g. "Apex Auto Systems"
        String customerTier,                // e.g. "TIER-1"
        LocalDateTime dueDate,              // Order delivery due date
        double regularRecoveryCost,         // Projected total cost of regular recovery (₹X)
        double regularLatePenalty,          // Incremental late penalty if regular shift is used
        String regularRecoveryTime,         // e.g. "Aug 31, 06:00 AM" (or ISO string)
        double overtimeRecoveryCost,        // Projected total cost of overtime recovery (₹Y)
        double overtimeLaborCost,           // Direct overtime labor cost (₹500/hr)
        double overtimeLatePenalty,         // Incremental late penalty if overtime is used
        String overtimeRecoveryTime,        // e.g. "Aug 30, 02:00 PM" (or ISO string)
        double savings,                     // ₹Z = regularRecoveryCost - overtimeRecoveryCost (when OT selected)
        String recommendation,              // Rationale message
        String details                      // Supervisor explanation
) {
    public static OvertimeDecisionComparison noDecisionRequired() {
        return new OvertimeDecisionComparison(
                "NO_DECISION_REQUIRED",
                null, null, null, null, null, null,
                0.0, 0.0, null,
                0.0, 0.0, 0.0, null,
                0.0,
                "No overtime decision required.",
                "Regular shift capacity is sufficient to recover the schedule without requiring overtime."
        );
    }
}
