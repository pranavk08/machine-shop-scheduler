package com.mirai.machineshop.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

import com.mirai.machineshop.entity.Operator;
import com.mirai.machineshop.entity.OperatorAbsence;
import com.mirai.machineshop.exception.GlobalExceptionHandler;
import com.mirai.machineshop.repository.OperatorAbsenceRepository;
import com.mirai.machineshop.repository.OperatorRepository;
import com.mirai.machineshop.service.OperatorAbsenceService;

class OperatorAbsenceControllerTests {

    private MockMvc mockMvc;
    private OperatorAbsenceRepository operatorAbsenceRepository;
    private OperatorRepository operatorRepository;

    @BeforeEach
    void setUp() {
        operatorAbsenceRepository = Mockito.mock(OperatorAbsenceRepository.class);
        operatorRepository = Mockito.mock(OperatorRepository.class);
        OperatorAbsenceService service = new OperatorAbsenceService(operatorAbsenceRepository, operatorRepository);
        OperatorAbsenceController controller = new OperatorAbsenceController(service);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getAllAbsences_returnsList() throws Exception {
        Operator op = new Operator("OP-001", "Ravi");
        ReflectionTestUtils.setField(op, "id", 1L);
        OperatorAbsence absence = new OperatorAbsence(op, LocalDateTime.now(), LocalDateTime.now().plusHours(4), "Leave");
        ReflectionTestUtils.setField(absence, "id", 100L);

        when(operatorAbsenceRepository.findAll()).thenReturn(List.of(absence));

        mockMvc.perform(get("/api/operator-absences"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(100L))
                .andExpect(jsonPath("$[0].reason").value("Leave"));
    }

    @Test
    void createAbsence_validRequest_createsAbsence() throws Exception {
        Operator op = new Operator("OP-001", "Ravi");
        ReflectionTestUtils.setField(op, "id", 1L);

        when(operatorRepository.findById(1L)).thenReturn(Optional.of(op));
        when(operatorAbsenceRepository.save(any(OperatorAbsence.class))).thenAnswer(inv -> {
            OperatorAbsence a = inv.getArgument(0);
            ReflectionTestUtils.setField(a, "id", 101L);
            return a;
        });

        String json = """
                {
                    "operatorId": 1,
                    "startTime": "2026-08-29T10:00:00",
                    "endTime": "2026-08-29T18:00:00",
                    "reason": "Emergency personal leave"
                }
                """;

        mockMvc.perform(post("/api/operator-absences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(101L))
                .andExpect(jsonPath("$.reason").value("Emergency personal leave"));
    }

    @Test
    void createAbsence_endTimeBeforeStartTime_returns400() throws Exception {
        String json = """
                {
                    "operatorId": 1,
                    "startTime": "2026-08-29T18:00:00",
                    "endTime": "2026-08-29T10:00:00",
                    "reason": "Invalid time"
                }
                """;

        mockMvc.perform(post("/api/operator-absences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteAbsence_existingId_returns204() throws Exception {
        when(operatorAbsenceRepository.existsById(100L)).thenReturn(true);

        mockMvc.perform(delete("/api/operator-absences/100"))
                .andExpect(status().isNoContent());
    }
}