package com.mirai.machineshop.dto;

import java.time.LocalDateTime;
import com.mirai.machineshop.scheduler.SchedulingStrategy;

public record ReplanRequest(
        LocalDateTime baselineStartTime,
        LocalDateTime replanStartTime,
        SchedulingStrategy strategy) {

    public ReplanRequest(LocalDateTime replanStartTime) {
        this(null, replanStartTime, null);
    }

    public ReplanRequest(LocalDateTime baselineStartTime, LocalDateTime replanStartTime) {
        this(baselineStartTime, replanStartTime, null);
    }
}
