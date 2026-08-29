package com.mirai.machineshop.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.mirai.machineshop.dto.BreakdownRequest;
import com.mirai.machineshop.dto.ReplanResultResponse;
import com.mirai.machineshop.entity.Breakdown;
import com.mirai.machineshop.entity.Machine;
import com.mirai.machineshop.exception.GlobalExceptionHandler;
import com.mirai.machineshop.repository.BreakdownRepository;
import com.mirai.machineshop.repository.MachineRepository;
import com.mirai.machineshop.scheduler.SchedulerService;
import com.mirai.machineshop.service.BreakdownService;

class BreakdownAndReplanControllerTests {

    private MockMvc breakdownMockMvc;
    private MockMvc schedulerMockMvc;

    private BreakdownRepository breakdownRepository;
    private MachineRepository machineRepository;
    private BreakdownService breakdownService;
    private SchedulerService schedulerService;

    @BeforeEach
    void setUp() {
        breakdownRepository = Mockito.mock(BreakdownRepository.class);
        machineRepository = Mockito.mock(MachineRepository.class);
        breakdownService = new BreakdownService(breakdownRepository, machineRepository);
        schedulerService = Mockito.mock(SchedulerService.class);

        BreakdownController breakdownController = new BreakdownController(breakdownService);
        SchedulerController schedulerController = new SchedulerController(schedulerService);

        breakdownMockMvc = MockMvcBuilders.standaloneSetup(breakdownController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        schedulerMockMvc = MockMvcBuilders.standaloneSetup(schedulerController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createBreakdown_validInput_returns201AndPersistsBreakdown() throws Exception {
        Machine machine = new Machine("CNC-01", "CNC Lathe", "TURNING");
        ReflectionTestUtils.setField(machine, "id", 1L);

        LocalDateTime start = LocalDateTime.of(2026, 8, 28, 10, 0);
        LocalDateTime end = LocalDateTime.of(2026, 8, 28, 14, 0);

        Breakdown breakdown = new Breakdown(machine, start, end, "Bearing overheating");
        ReflectionTestUtils.setField(breakdown, "id", 101L);

        when(machineRepository.findById(1L)).thenReturn(Optional.of(machine));
        when(breakdownRepository.save(any(Breakdown.class))).thenReturn(breakdown);

        String json = """
                {
                    "machineId": 1,
                    "startTime": "2026-08-28T10:00:00",
                    "endTime": "2026-08-28T14:00:00",
                    "reason": "Bearing overheating"
                }
                """;

        breakdownMockMvc.perform(post("/api/breakdowns")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(101))
                .andExpect(jsonPath("$.reason").value("Bearing overheating"))
                .andExpect(jsonPath("$.machine.machineCode").value("CNC-01"));

        verify(breakdownRepository).save(any(Breakdown.class));
    }

    @Test
    void createBreakdown_missingFields_returns400() throws Exception {
        String invalidJson = """
                {
                    "machineId": null,
                    "startTime": null,
                    "endTime": null,
                    "reason": ""
                }
                """;

        breakdownMockMvc.perform(post("/api/breakdowns")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Request validation failed."));
    }

    @Test
    void createBreakdown_endTimeBeforeStartTime_returns400() throws Exception {
        Machine machine = new Machine("CNC-01", "CNC Lathe", "TURNING");
        ReflectionTestUtils.setField(machine, "id", 1L);

        when(machineRepository.findById(1L)).thenReturn(Optional.of(machine));

        String json = """
                {
                    "machineId": 1,
                    "startTime": "2026-08-28T14:00:00",
                    "endTime": "2026-08-28T10:00:00",
                    "reason": "Reversed times"
                }
                """;

        breakdownMockMvc.perform(post("/api/breakdowns")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("endTime must be after startTime."));
    }

    @Test
    void createBreakdown_unknownMachine_returns404() throws Exception {
        when(machineRepository.findById(999L)).thenReturn(Optional.empty());

        String json = """
                {
                    "machineId": 999,
                    "startTime": "2026-08-28T10:00:00",
                    "endTime": "2026-08-28T14:00:00",
                    "reason": "Unknown machine"
                }
                """;

        breakdownMockMvc.perform(post("/api/breakdowns")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Machine not found: 999"));
    }

    @Test
    void replanEndpoint_validRequest_returns200WithReplanResult() throws Exception {
        LocalDateTime replanTime = LocalDateTime.of(2026, 8, 28, 10, 0);
        ReplanResultResponse mockResponse = new ReplanResultResponse(
                replanTime,
                10,
                2,
                1,
                1,
                1,
                List.of(),
                List.of(),
                List.of()
        );

        when(schedulerService.replanSchedule(any(), any(), any())).thenReturn(mockResponse);

        schedulerMockMvc.perform(post("/api/scheduler/replan")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"replanStartTime\":\"2026-08-28T10:00:00\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalOperations").value(10))
                .andExpect(jsonPath("$.operationsMovedCount").value(2))
                .andExpect(jsonPath("$.ordersDelayedCount").value(1))
                .andExpect(jsonPath("$.machinesReassignedCount").value(1));
    }

    @Test
    void replanEndpoint_withStrategy_passesStrategyToService() throws Exception {
        ReplanResultResponse mockResponse = new ReplanResultResponse(
                LocalDateTime.now(), 5, 1, 0, 0, 0, List.of(), List.of(), List.of()
        );

        when(schedulerService.replanSchedule(Mockito.eq(com.mirai.machineshop.scheduler.SchedulingStrategy.CHEAPEST_PRODUCTION), any(), any()))
                .thenReturn(mockResponse);

        schedulerMockMvc.perform(post("/api/scheduler/replan")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"strategy\":\"CHEAPEST_PRODUCTION\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalOperations").value(5));
    }
}
