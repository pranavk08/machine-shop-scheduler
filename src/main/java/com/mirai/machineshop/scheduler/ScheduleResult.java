package com.mirai.machineshop.scheduler;

import com.mirai.machineshop.entity.Machine;
import com.mirai.machineshop.entity.Operation;

import java.time.LocalDateTime;
import com.mirai.machineshop.entity.Operator;

public class ScheduleResult {

    private Operation operation;
    private Machine machine;
    private Operator operator;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    public ScheduleResult(

            Operation operation,

            Machine machine,

            Operator operator,

            LocalDateTime startTime,

            LocalDateTime endTime) {

        this.operation = operation;

        this.machine = machine;

        this.operator = operator;

        this.startTime = startTime;

        this.endTime = endTime;
    }
    
    public Operator getOperator() {

        return operator;
    }

    public Operation getOperation() {
        return operation;
    }

    public Machine getMachine() {
        return machine;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }
}