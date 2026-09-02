package com.mirai.machineshop.dto;

import java.time.LocalDateTime;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record MaterialDelayRequest(
        @NotNull(message = "orderId is required")
        @Positive(message = "orderId must be a positive number")
        Long orderId,

        @NotNull(message = "delayedUntil is required")
        LocalDateTime delayedUntil,

        @NotBlank(message = "reason is required")
        @Size(max = 255, message = "reason must be at most 255 characters")
        String reason) {
}
