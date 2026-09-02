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

import com.mirai.machineshop.entity.Customer;
import com.mirai.machineshop.entity.MaterialDelay;
import com.mirai.machineshop.entity.Order;
import com.mirai.machineshop.exception.GlobalExceptionHandler;
import com.mirai.machineshop.repository.MaterialDelayRepository;
import com.mirai.machineshop.repository.OrderRepository;
import com.mirai.machineshop.service.MaterialDelayService;

class MaterialDelayControllerTests {

    private MockMvc mockMvc;
    private MaterialDelayRepository materialDelayRepository;
    private OrderRepository orderRepository;

    @BeforeEach
    void setUp() {
        materialDelayRepository = Mockito.mock(MaterialDelayRepository.class);
        orderRepository = Mockito.mock(OrderRepository.class);
        MaterialDelayService service = new MaterialDelayService(materialDelayRepository, orderRepository);
        MaterialDelayController controller = new MaterialDelayController(service);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getAllMaterialDelays_returnsList() throws Exception {
        Customer cust = new Customer("CUST-1", "Acme Corp", "Tier 1");
        Order order = new Order("ORD-001", cust, 10, "SHAFT", LocalDateTime.now().plusDays(5), "OPEN");
        ReflectionTestUtils.setField(order, "id", 1L);
        MaterialDelay delay = new MaterialDelay(order, LocalDateTime.now().plusDays(1), "Shipment delayed");
        ReflectionTestUtils.setField(delay, "id", 200L);

        when(materialDelayRepository.findAll()).thenReturn(List.of(delay));

        mockMvc.perform(get("/api/material-delays"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(200L))
                .andExpect(jsonPath("$[0].reason").value("Shipment delayed"));
    }

    @Test
    void createMaterialDelay_validRequest_createsDelay() throws Exception {
        Customer cust = new Customer("CUST-1", "Acme Corp", "Tier 1");
        Order order = new Order("ORD-001", cust, 10, "SHAFT", LocalDateTime.now().plusDays(5), "OPEN");
        ReflectionTestUtils.setField(order, "id", 1L);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(materialDelayRepository.save(any(MaterialDelay.class))).thenAnswer(inv -> {
            MaterialDelay md = inv.getArgument(0);
            ReflectionTestUtils.setField(md, "id", 201L);
            return md;
        });

        String json = """
                {
                    "orderId": 1,
                    "delayedUntil": "2026-08-30T14:00:00",
                    "reason": "Raw material supplier shipment delay"
                }
                """;

        mockMvc.perform(post("/api/material-delays")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(201L))
                .andExpect(jsonPath("$.reason").value("Raw material supplier shipment delay"));
    }

    @Test
    void createMaterialDelay_missingOrderId_returns400() throws Exception {
        String json = """
                {
                    "delayedUntil": "2026-08-30T14:00:00",
                    "reason": "Missing order"
                }
                """;

        mockMvc.perform(post("/api/material-delays")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createMaterialDelay_blankReason_returns400() throws Exception {
        String json = """
                {
                    "orderId": 1,
                    "delayedUntil": "2026-08-30T14:00:00",
                    "reason": "   "
                }
                """;

        mockMvc.perform(post("/api/material-delays")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteMaterialDelay_existingId_returns204() throws Exception {
        when(materialDelayRepository.existsById(200L)).thenReturn(true);

        mockMvc.perform(delete("/api/material-delays/200"))
                .andExpect(status().isNoContent());

        Mockito.verify(materialDelayRepository).deleteById(200L);
    }
}
