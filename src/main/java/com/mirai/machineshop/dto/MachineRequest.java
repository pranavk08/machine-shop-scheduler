package com.mirai.machineshop.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MachineRequest(
        @NotBlank(message = "machineCode is required")
        @Size(max = 100, message = "machineCode must be at most 100 characters")
        String machineCode,

        @NotBlank(message = "name is required")
        @Size(max = 255, message = "name must be at most 255 characters")
        String name,

        @NotBlank(message = "type is required")
        @Size(max = 100, message = "type must be at most 100 characters")
        String type,

        Boolean available) {
}
