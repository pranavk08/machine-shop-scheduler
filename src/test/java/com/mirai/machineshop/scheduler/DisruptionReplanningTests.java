package com.mirai.machineshop.scheduler;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.mirai.machineshop.dto.OperationScheduleDelta;
import com.mirai.machineshop.dto.ReplanResultResponse;
import com.mirai.machineshop.entity.Breakdown;
import com.mirai.machineshop.entity.Machine;
import com.mirai.machineshop.entity.MachineCapability;
import com.mirai.machineshop.entity.Operation;
import com.mirai.machineshop.entity.Operator;
import com.mirai.machineshop.entity.OperatorShift;
import com.mirai.machineshop.entity.OperatorSkill;
import com.mirai.machineshop.entity.Order;
import com.mirai.machineshop.entity.Shift;
import com.mirai.machineshop.repository.BreakdownRepository;
import com.mirai.machineshop.repository.ChangeoverRepository;
import com.mirai.machineshop.repository.MachineCapabilityRepository;
import com.mirai.machineshop.repository.OperationRepository;
import com.mirai.machineshop.repository.OperatorShiftRepository;
import com.mirai.machineshop.repository.OperatorSkillRepository;
import com.mirai.machineshop.repository.OrderRepository;

class DisruptionReplanningTests {

    private MachineCapabilityRepository machineCapabilityRepository;
    private OperationRepository operationRepository;
    private OrderRepository orderRepository;
    private OperatorSkillRepository operatorSkillRepository;
    private OperatorShiftRepository operatorShiftRepository;
    private ChangeoverRepository changeoverRepository;
    private BreakdownRepository breakdownRepository;
    private SchedulerService schedulerService;

    private Machine lathe1;
    private Machine lathe2;
    private Machine grinder1;
    private Operator operatorRavi;
    private Operator operatorKumar;
    private Shift fullDayShift;

    @BeforeEach
    void setUp() {
        machineCapabilityRepository = mock(MachineCapabilityRepository.class);
        operationRepository = mock(OperationRepository.class);
        orderRepository = mock(OrderRepository.class);
        operatorSkillRepository = mock(OperatorSkillRepository.class);
        operatorShiftRepository = mock(OperatorShiftRepository.class);
        changeoverRepository = mock(ChangeoverRepository.class);
        breakdownRepository = mock(BreakdownRepository.class);

        schedulerService = new SchedulerService(
                machineCapabilityRepository,
                operationRepository,
                orderRepository,
                operatorSkillRepository,
                operatorShiftRepository,
                changeoverRepository,
                breakdownRepository);

        lathe1 = new Machine("CNC-01", "CNC Lathe 1", "TURNING");
        ReflectionTestUtils.setField(lathe1, "id", 1L);

        lathe2 = new Machine("CNC-02", "CNC Lathe 2", "TURNING");
        ReflectionTestUtils.setField(lathe2, "id", 2L);

        grinder1 = new Machine("GRIND-01", "Cylindrical Grinder", "GRINDING");
        ReflectionTestUtils.setField(grinder1, "id", 3L);

        operatorRavi = new Operator("OP-001", "Ravi");
        ReflectionTestUtils.setField(operatorRavi, "id", 1L);

        operatorKumar = new Operator("OP-002", "Kumar");
        ReflectionTestUtils.setField(operatorKumar, "id", 2L);

        fullDayShift = new Shift("SHIFT-ALL", LocalTime.of(0, 0), LocalTime.of(23, 59));
        ReflectionTestUtils.setField(fullDayShift, "id", 1L);

        when(changeoverRepository.findByMachineIdAndFromPartFamilyIgnoreCaseAndToPartFamilyIgnoreCase(
                any(), any(), any())).thenReturn(Optional.empty());
    }

    @Test
    void replanSchedule_reroutesOperationToAlternativeMachine() {
        LocalDate today = LocalDate.now();
        LocalDateTime replanStart = LocalDateTime.of(today, LocalTime.of(10, 0));

        Order order = new Order("ORD-001", null, 10, "SHAFT", replanStart.plusDays(2), "OPEN");
        ReflectionTestUtils.setField(order, "id", 100L);

        Operation turningOp = new Operation(order, 1, "TURNING", 60, "TURNING");
        ReflectionTestUtils.setField(turningOp, "id", 1001L);

        when(orderRepository.findByStatusIgnoreCase("OPEN")).thenReturn(List.of(order));
        when(orderRepository.existsById(100L)).thenReturn(true);
        when(operationRepository.findAll()).thenReturn(List.of(turningOp));

        // Capabilities: both lathe1 and lathe2 can turn
        when(machineCapabilityRepository.findByCapabilityIgnoreCase("TURNING"))
                .thenReturn(List.of(
                        new MachineCapability(lathe1, "TURNING"),
                        new MachineCapability(lathe2, "TURNING")));

        // Operators: Ravi has turning skill
        when(operatorSkillRepository.findBySkillNameIgnoreCase("TURNING"))
                .thenReturn(List.of(new OperatorSkill(operatorRavi, "TURNING")));

        OperatorShift shift = new OperatorShift(operatorRavi, fullDayShift, today, true);
        when(operatorShiftRepository.findByOperatorIdAndWorkDateAndAvailableTrue(any(), any()))
                .thenReturn(List.of(shift));

        // Active breakdown on lathe1 from 10:00 to 14:00
        Breakdown lathe1Breakdown = new Breakdown(lathe1, replanStart, replanStart.plusHours(4), "Spindle failure");
        when(breakdownRepository.findByMachineId(1L)).thenReturn(List.of(lathe1Breakdown));
        when(breakdownRepository.findByMachineId(2L)).thenReturn(List.of());

        ReplanResultResponse response = schedulerService.replanSchedule(replanStart);

        assertNotNull(response);
        assertEquals(1, response.totalOperations());
        assertEquals(1, response.operationsMovedCount());
        assertEquals(1, response.machinesReassignedCount());
        assertEquals(0, response.ordersDelayedCount());

        // Operation in afterSchedule should be rerouted to CNC-02
        ScheduleResult afterRes = response.afterSchedule().get(0);
        assertEquals("CNC-02", afterRes.getMachine().getMachineCode());
        assertEquals(replanStart, afterRes.getStartTime());

        // Delta check
        OperationScheduleDelta delta = response.impactDeltas().get(0);
        assertEquals("CNC-01", delta.beforeMachineCode());
        assertEquals("CNC-02", delta.afterMachineCode());
        assertTrue(delta.machineChanged());
        assertFalse(delta.timeChanged());
    }

    @Test
    void replanSchedule_singleMachineBottleneck_waitsUntilBreakdownEnds() {
        LocalDate today = LocalDate.now();
        LocalDateTime replanStart = LocalDateTime.of(today, LocalTime.of(10, 0));

        Order order = new Order("ORD-GRIND", null, 10, "SHAFT", replanStart.plusDays(2), "OPEN");
        ReflectionTestUtils.setField(order, "id", 200L);

        Operation grindOp = new Operation(order, 1, "GRINDING", 60, "GRINDING");
        ReflectionTestUtils.setField(grindOp, "id", 2001L);

        when(orderRepository.findByStatusIgnoreCase("OPEN")).thenReturn(List.of(order));
        when(orderRepository.existsById(200L)).thenReturn(true);
        when(operationRepository.findAll()).thenReturn(List.of(grindOp));

        // Only grinder1 is capable
        when(machineCapabilityRepository.findByCapabilityIgnoreCase("GRINDING"))
                .thenReturn(List.of(new MachineCapability(grinder1, "GRINDING")));

        when(operatorSkillRepository.findBySkillNameIgnoreCase("GRINDING"))
                .thenReturn(List.of(new OperatorSkill(operatorKumar, "GRINDING")));

        OperatorShift shift = new OperatorShift(operatorKumar, fullDayShift, today, true);
        when(operatorShiftRepository.findByOperatorIdAndWorkDateAndAvailableTrue(any(), any()))
                .thenReturn(List.of(shift));

        // Breakdown on grinder1 from 10:00 to 14:00 (4 hours)
        LocalDateTime breakdownEnd = replanStart.plusHours(4);
        Breakdown grindBreakdown = new Breakdown(grinder1, replanStart, breakdownEnd, "Wheel alignment");
        when(breakdownRepository.findByMachineId(3L)).thenReturn(List.of(grindBreakdown));

        ReplanResultResponse response = schedulerService.replanSchedule(replanStart);

        assertNotNull(response);
        assertEquals(1, response.totalOperations());
        assertEquals(1, response.operationsMovedCount());
        assertEquals(0, response.machinesReassignedCount()); // Machine is still GRIND-01
        assertEquals(1, response.ordersDelayedCount()); // Delayed by 4 hours

        ScheduleResult afterRes = response.afterSchedule().get(0);
        assertEquals("GRIND-01", afterRes.getMachine().getMachineCode());
        // Should start immediately at breakdown end (14:00)
        assertEquals(breakdownEnd, afterRes.getStartTime());
        assertEquals(breakdownEnd.plusMinutes(60), afterRes.getEndTime());

        OperationScheduleDelta delta = response.impactDeltas().get(0);
        assertFalse(delta.machineChanged());
        assertTrue(delta.timeChanged());
        assertEquals(240, delta.delayMinutes()); // 4 hours delay
    }

    @Test
    void replanSchedule_operationsBeforeReplanStartTimeRemainUnchanged() {
        LocalDate today = LocalDate.now();
        LocalDateTime baseTime = LocalDateTime.of(today, LocalTime.of(8, 0));
        LocalDateTime replanStart = LocalDateTime.of(today, LocalTime.of(12, 0));

        Order order = new Order("ORD-MULTI", null, 10, "SHAFT", baseTime.plusDays(2), "OPEN");
        ReflectionTestUtils.setField(order, "id", 300L);

        // Op 1 runs 08:00 to 09:30 (finishes before replanStart 12:00)
        Operation op1 = new Operation(order, 1, "TURNING", 90, "TURNING");
        ReflectionTestUtils.setField(op1, "id", 3001L);

        // Op 2 runs 09:30 to 13:30 (overlaps or occurs past replanStart 12:00)
        Operation op2 = new Operation(order, 2, "TURNING", 240, "TURNING");
        ReflectionTestUtils.setField(op2, "id", 3002L);

        when(orderRepository.findByStatusIgnoreCase("OPEN")).thenReturn(List.of(order));
        when(orderRepository.existsById(300L)).thenReturn(true);
        when(operationRepository.findAll()).thenReturn(List.of(op1, op2));

        when(machineCapabilityRepository.findByCapabilityIgnoreCase("TURNING"))
                .thenReturn(List.of(
                        new MachineCapability(lathe1, "TURNING"),
                        new MachineCapability(lathe2, "TURNING")));

        when(operatorSkillRepository.findBySkillNameIgnoreCase("TURNING"))
                .thenReturn(List.of(new OperatorSkill(operatorRavi, "TURNING")));

        OperatorShift shift = new OperatorShift(operatorRavi, fullDayShift, today, true);
        when(operatorShiftRepository.findByOperatorIdAndWorkDateAndAvailableTrue(any(), any()))
                .thenReturn(List.of(shift));

        // Breakdown on lathe1 starts at 12:00
        Breakdown breakdown = new Breakdown(lathe1, replanStart, replanStart.plusHours(4), "Motor failure");
        when(breakdownRepository.findByMachineId(1L)).thenReturn(List.of(breakdown));
        when(breakdownRepository.findByMachineId(2L)).thenReturn(List.of());

        ReplanResultResponse response = schedulerService.replanSchedule(baseTime, replanStart);

        assertNotNull(response);
        assertEquals(2, response.totalOperations());

        // Op 1 (completed at 09:30 before 12:00) must remain on CNC-01 at 08:00
        ScheduleResult afterOp1 = response.afterSchedule().stream()
                .filter(res -> res.getOperation().getId().equals(3001L))
                .findFirst().orElseThrow();
        assertEquals("CNC-01", afterOp1.getMachine().getMachineCode());
        assertEquals(baseTime, afterOp1.getStartTime());

        // Op 2 (scheduled at 12:00) must be rerouted to CNC-02 because CNC-01 is broken
        ScheduleResult afterOp2 = response.afterSchedule().stream()
                .filter(res -> res.getOperation().getId().equals(3002L))
                .findFirst().orElseThrow();
        assertEquals("CNC-02", afterOp2.getMachine().getMachineCode());
    }

    @Test
    void replanSchedule_concurrentRequests_noConcurrentModificationException() throws Exception {
        LocalDate today = LocalDate.now();
        LocalDateTime replanStart = LocalDateTime.of(today, LocalTime.of(10, 0));

        Order order = new Order("ORD-CONCURRENT", null, 10, "SHAFT", replanStart.plusDays(2), "OPEN");
        ReflectionTestUtils.setField(order, "id", 400L);

        Operation op = new Operation(order, 1, "TURNING", 60, "TURNING");
        ReflectionTestUtils.setField(op, "id", 4001L);

        when(orderRepository.findByStatusIgnoreCase("OPEN")).thenReturn(List.of(order));
        when(orderRepository.existsById(400L)).thenReturn(true);
        when(operationRepository.findAll()).thenReturn(List.of(op));

        when(machineCapabilityRepository.findByCapabilityIgnoreCase("TURNING"))
                .thenReturn(List.of(new MachineCapability(lathe1, "TURNING")));

        when(operatorSkillRepository.findBySkillNameIgnoreCase("TURNING"))
                .thenReturn(List.of(new OperatorSkill(operatorRavi, "TURNING")));

        OperatorShift shift = new OperatorShift(operatorRavi, fullDayShift, today, true);
        when(operatorShiftRepository.findByOperatorIdAndWorkDateAndAvailableTrue(any(), any()))
                .thenReturn(List.of(shift));
        when(breakdownRepository.findByMachineId(any())).thenReturn(List.of());

        ExecutorService executor = Executors.newFixedThreadPool(8);
        List<Callable<ReplanResultResponse>> tasks = new ArrayList<>();

        for (int i = 0; i < 20; i++) {
            tasks.add(() -> schedulerService.replanSchedule(replanStart));
        }

        assertDoesNotThrow(() -> {
            List<Future<ReplanResultResponse>> futures = executor.invokeAll(tasks);
            for (Future<ReplanResultResponse> future : futures) {
                assertNotNull(future.get());
            }
        });

        executor.shutdown();
    }
}
