package com.mirai.machineshop.scheduler;

import java.time.LocalDateTime;

import com.mirai.machineshop.entity.Machine;
import com.mirai.machineshop.entity.Operator;

public class MachineAvailability {

    private Machine machine;

    private Operator operator;

    private LocalDateTime startTime;

    private LocalDateTime operationStartTime;

    public MachineAvailability(

            Machine machine,

            Operator operator,

            LocalDateTime startTime,

            LocalDateTime operationStartTime) {

        this.machine = machine;

        this.operator = operator;

        this.startTime = startTime;

        this.operationStartTime = operationStartTime;
    }

    public Machine getMachine() {

        return machine;
    }

    public Operator getOperator() {

        return operator;
    }

    public LocalDateTime getStartTime() {

        return startTime;
    }

    public LocalDateTime getOperationStartTime() {

        return operationStartTime;
    }
}