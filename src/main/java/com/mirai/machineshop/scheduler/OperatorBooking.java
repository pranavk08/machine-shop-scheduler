package com.mirai.machineshop.scheduler;

import java.time.LocalDateTime;

import com.mirai.machineshop.entity.Operator;

public class OperatorBooking {

    private Operator operator;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    public OperatorBooking(
            Operator operator,
            LocalDateTime startTime,
            LocalDateTime endTime) {

        this.operator = operator;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public Operator getOperator() {
        return operator;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }
}