package com.mirai.machineshop.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OperatorRequest(
        @NotBlank(message = "operatorCode is required")
        @Size(max = 100, message = "operatorCode must be at most 100 characters")
        String operatorCode,

        @NotBlank(message = "name is required")
        @Size(max = 255, message = "name must be at most 255 characters")
        String name,

        Boolean available) {
}
