package com.mirai.machineshop.scheduler;

import java.time.LocalDateTime;

import com.mirai.machineshop.entity.Machine;

public class MachineBooking {

    private Machine machine;

    private String partFamily;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    public MachineBooking(
            Machine machine,
            String partFamily,
            LocalDateTime startTime,
            LocalDateTime endTime) {

        this.machine = machine;
        this.partFamily = partFamily;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public Machine getMachine() {
        return machine;
    }

    public String getPartFamily() {
        return partFamily;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }
}