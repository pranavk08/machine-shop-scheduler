package com.mirai.machineshop.dto;

import java.time.LocalDateTime;

public record ReplanRequest(
        LocalDateTime baselineStartTime,
        LocalDateTime replanStartTime) {

    public ReplanRequest(LocalDateTime replanStartTime) {
        this(null, replanStartTime);
    }
}
