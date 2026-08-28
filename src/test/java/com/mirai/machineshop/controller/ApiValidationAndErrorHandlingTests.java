package com.mirai.machineshop.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.mirai.machineshop.exception.DuplicateResourceException;
import com.mirai.machineshop.exception.GlobalExceptionHandler;
import com.mirai.machineshop.exception.ResourceNotFoundException;
import com.mirai.machineshop.exception.SchedulingUnavailableException;
import com.mirai.machineshop.scheduler.SchedulerService;
import com.mirai.machineshop.service.MachineService;

class ApiValidationAndErrorHandlingTests {

    private MachineService machineService;
    private SchedulerService schedulerService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        machineService = mock(MachineService.class);
        schedulerService = mock(SchedulerService.class);

        mockMvc = MockMvcBuilders
                .standaloneSetup(
                        new MachineController(machineService),
                        new SchedulerController(schedulerService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void rejectsMachineRequestWithMissingRequiredFields() throws Exception {
        mockMvc.perform(post("/api/machines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"machineCode\":\"\",\"name\":\"\",\"type\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.machineCode").exists())
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.path").value("/api/machines"));

        verifyNoInteractions(machineService);
    }

    @Test
    void mapsDuplicateMachineCodeToConflict() throws Exception {
        when(machineService.createMachine(any())).thenThrow(
                new DuplicateResourceException("Machine code already exists: M-001"));

        mockMvc.perform(post("/api/machines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"machineCode\":\"M-001\",\"name\":\"Lathe\",\"type\":\"TURNING\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_RESOURCE"))
                .andExpect(jsonPath("$.message").value("Machine code already exists: M-001"));
    }

    @Test
    void mapsUnknownMachineToNotFound() throws Exception {
        when(machineService.getMachineById(99L)).thenThrow(
                new ResourceNotFoundException("Machine not found: 99"));

        mockMvc.perform(get("/api/machines/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void mapsDatabaseIntegrityFailuresWithoutInternalDetails() throws Exception {
        when(machineService.createMachine(any())).thenThrow(
                new DataIntegrityViolationException("database-specific detail"));

        mockMvc.perform(post("/api/machines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"machineCode\":\"M-001\",\"name\":\"Lathe\",\"type\":\"TURNING\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DATA_INTEGRITY_VIOLATION"))
                .andExpect(jsonPath("$.message").value("The request conflicts with existing data."));
    }

    @Test
    void rejectsInvalidSchedulerTimeRange() throws Exception {
        mockMvc.perform(get("/api/scheduler/operators/available")
                        .queryParam("skill", "TURNING")
                        .queryParam("date", "2026-08-28")
                        .queryParam("start", "2026-08-28T10:00:00")
                        .queryParam("end", "2026-08-28T09:00:00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("end must be after start."));

        verifyNoInteractions(schedulerService);
    }

    @Test
    void mapsNoFeasibleScheduleToUnprocessableContent() throws Exception {
        when(schedulerService.scheduleAllOpenOrders()).thenThrow(
                new SchedulingUnavailableException("No open orders found."));

        mockMvc.perform(get("/api/scheduler/orders/schedule"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("SCHEDULING_UNAVAILABLE"));
    }
}
