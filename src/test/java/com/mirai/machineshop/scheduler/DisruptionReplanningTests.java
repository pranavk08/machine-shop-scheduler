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
    private Machine mill1;
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

        mill1 = new Machine("MILL-01", "Milling Center", "MILLING");
        ReflectionTestUtils.setField(mill1, "id", 4L);

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

        when(machineCapabilityRepository.findByCapabilityIgnoreCase("TURNING"))
                .thenReturn(List.of(
                        new MachineCapability(lathe1, "TURNING"),
                        new MachineCapability(lathe2, "TURNING")));

        when(operatorSkillRepository.findBySkillNameIgnoreCase("TURNING"))
                .thenReturn(List.of(new OperatorSkill(operatorRavi, "TURNING")));

        OperatorShift shift = new OperatorShift(operatorRavi, fullDayShift, today, true);
        when(operatorShiftRepository.findByOperatorIdAndWorkDateAndAvailableTrue(any(), any()))
                .thenReturn(List.of(shift));

        Breakdown lathe1Breakdown = new Breakdown(lathe1, replanStart, replanStart.plusHours(4), "Spindle failure");
        when(breakdownRepository.findByMachineId(1L)).thenReturn(List.of(lathe1Breakdown));
        when(breakdownRepository.findByMachineId(2L)).thenReturn(List.of());

        ReplanResultResponse response = schedulerService.replanSchedule(replanStart);

        assertNotNull(response);
        assertEquals(1, response.totalOperations());
        assertEquals(1, response.operationsMovedCount());
        assertEquals(1, response.machinesReassignedCount());
        assertEquals(0, response.ordersDelayedCount());

        ScheduleResult afterRes = response.afterSchedule().get(0);
        assertEquals("CNC-02", afterRes.getMachine().getMachineCode());
        assertEquals(replanStart, afterRes.getStartTime());

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

        when(machineCapabilityRepository.findByCapabilityIgnoreCase("GRINDING"))
                .thenReturn(List.of(new MachineCapability(grinder1, "GRINDING")));

        when(operatorSkillRepository.findBySkillNameIgnoreCase("GRINDING"))
                .thenReturn(List.of(new OperatorSkill(operatorKumar, "GRINDING")));

        OperatorShift shift = new OperatorShift(operatorKumar, fullDayShift, today, true);
        when(operatorShiftRepository.findByOperatorIdAndWorkDateAndAvailableTrue(any(), any()))
                .thenReturn(List.of(shift));

        LocalDateTime breakdownEnd = replanStart.plusHours(4);
        Breakdown grindBreakdown = new Breakdown(grinder1, replanStart, breakdownEnd, "Wheel alignment");
        when(breakdownRepository.findByMachineId(3L)).thenReturn(List.of(grindBreakdown));

        ReplanResultResponse response = schedulerService.replanSchedule(replanStart);

        assertNotNull(response);
        assertEquals(1, response.totalOperations());
        assertEquals(1, response.operationsMovedCount());
        assertEquals(0, response.machinesReassignedCount());
        assertEquals(1, response.ordersDelayedCount());

        ScheduleResult afterRes = response.afterSchedule().get(0);
        assertEquals("GRIND-01", afterRes.getMachine().getMachineCode());
        assertEquals(breakdownEnd, afterRes.getStartTime());
        assertEquals(breakdownEnd.plusMinutes(60), afterRes.getEndTime());

        OperationScheduleDelta delta = response.impactDeltas().get(0);
        assertFalse(delta.machineChanged());
        assertTrue(delta.timeChanged());
        assertEquals(240, delta.delayMinutes());
    }

    @Test
    void replanSchedule_operationsBeforeReplanStartTimeRemainUnchanged() {
        LocalDate today = LocalDate.now();
        LocalDateTime baseTime = LocalDateTime.of(today, LocalTime.of(8, 0));
        LocalDateTime replanStart = LocalDateTime.of(today, LocalTime.of(12, 0));

        Order order = new Order("ORD-MULTI", null, 10, "SHAFT", baseTime.plusDays(2), "OPEN");
        ReflectionTestUtils.setField(order, "id", 300L);

        Operation op1 = new Operation(order, 1, "TURNING", 90, "TURNING");
        ReflectionTestUtils.setField(op1, "id", 3001L);

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

        Breakdown breakdown = new Breakdown(lathe1, replanStart, replanStart.plusHours(4), "Motor failure");
        when(breakdownRepository.findByMachineId(1L)).thenReturn(List.of(breakdown));
        when(breakdownRepository.findByMachineId(2L)).thenReturn(List.of());

        ReplanResultResponse response = schedulerService.replanSchedule(baseTime, replanStart);

        assertNotNull(response);
        assertEquals(2, response.totalOperations());

        ScheduleResult afterOp1 = response.afterSchedule().stream()
                .filter(res -> res.getOperation().getId().equals(3001L))
                .findFirst().orElseThrow();
        assertEquals("CNC-01", afterOp1.getMachine().getMachineCode());
        assertEquals(baseTime, afterOp1.getStartTime());

        ScheduleResult afterOp2 = response.afterSchedule().stream()
                .filter(res -> res.getOperation().getId().equals(3002L))
                .findFirst().orElseThrow();
        assertEquals("CNC-02", afterOp2.getMachine().getMachineCode());
    }

    @Test
    void replanSchedule_intermediateOpDelayed_finalOrderDeliveryUnchanged() {
        LocalDate today = LocalDate.now();
        LocalDateTime replanStart = LocalDateTime.of(today, LocalTime.of(10, 0));

        // Order 1 has 2 operations: Op 1 (TURNING 60m) and Op 2 (GRINDING 60m)
        Order order1 = new Order("ORD-001", null, 10, "SHAFT", replanStart.plusDays(2), "OPEN");
        ReflectionTestUtils.setField(order1, "id", 501L);

        Operation op1 = new Operation(order1, 1, "TURNING", 60, "TURNING");
        ReflectionTestUtils.setField(op1, "id", 5001L);

        Operation op2 = new Operation(order1, 2, "GRINDING", 60, "GRINDING");
        ReflectionTestUtils.setField(op2, "id", 5002L);

        // Order 2 reserves grinder1 from 10:00 to 12:00, so Order 1's Op 2 can only start at 12:00 in baseline
        Order order2 = new Order("ORD-002", null, 10, "SHAFT", replanStart.plusDays(1), "OPEN");
        ReflectionTestUtils.setField(order2, "id", 502L);
        Operation opOrder2 = new Operation(order2, 1, "GRINDING", 120, "GRINDING");
        ReflectionTestUtils.setField(opOrder2, "id", 5003L);

        when(orderRepository.findByStatusIgnoreCase("OPEN")).thenReturn(List.of(order2, order1));
        when(orderRepository.existsById(501L)).thenReturn(true);
        when(orderRepository.existsById(502L)).thenReturn(true);
        when(operationRepository.findAll()).thenReturn(List.of(opOrder2, op1, op2));

        when(machineCapabilityRepository.findByCapabilityIgnoreCase("TURNING"))
                .thenReturn(List.of(new MachineCapability(lathe1, "TURNING")));
        when(machineCapabilityRepository.findByCapabilityIgnoreCase("GRINDING"))
                .thenReturn(List.of(new MachineCapability(grinder1, "GRINDING")));

        when(operatorSkillRepository.findBySkillNameIgnoreCase("TURNING"))
                .thenReturn(List.of(new OperatorSkill(operatorRavi, "TURNING")));
        when(operatorSkillRepository.findBySkillNameIgnoreCase("GRINDING"))
                .thenReturn(List.of(new OperatorSkill(operatorKumar, "GRINDING")));

        OperatorShift shiftRavi = new OperatorShift(operatorRavi, fullDayShift, today, true);
        OperatorShift shiftKumar = new OperatorShift(operatorKumar, fullDayShift, today, true);
        when(operatorShiftRepository.findByOperatorIdAndWorkDateAndAvailableTrue(any(), any()))
                .thenAnswer(inv -> {
                    Long opId = inv.getArgument(0);
                    return opId.equals(1L) ? List.of(shiftRavi) : List.of(shiftKumar);
                });

        // 30 minute breakdown on lathe1 (10:00 - 10:30)
        Breakdown minorBreakdown = new Breakdown(lathe1, replanStart, replanStart.plusMinutes(30), "Tool change delay");
        when(breakdownRepository.findByMachineId(1L)).thenReturn(List.of(minorBreakdown));
        when(breakdownRepository.findByMachineId(3L)).thenReturn(List.of());

        ReplanResultResponse response = schedulerService.replanSchedule(replanStart);

        assertNotNull(response);
        // op1 is shifted from 10:00-11:00 to 10:30-11:30 (operationsMovedCount = 1)
        assertEquals(1, response.operationsMovedCount());
        // op2 still starts at 12:00 and ends at 13:00 (after order2 finishes on grinder1), so order1 completion is unchanged!
        assertEquals(0, response.ordersDelayedCount());

        ScheduleResult afterOp1 = response.afterSchedule().stream()
                .filter(res -> res.getOperation().getId().equals(5001L)).findFirst().orElseThrow();
        assertEquals(replanStart.plusMinutes(30), afterOp1.getStartTime());

        ScheduleResult afterOp2 = response.afterSchedule().stream()
                .filter(res -> res.getOperation().getId().equals(5002L)).findFirst().orElseThrow();
        assertEquals(replanStart.plusHours(2), afterOp2.getStartTime());
    }

    @Test
    void replanSchedule_simultaneousMachineAndOperatorChange() {
        LocalDate today = LocalDate.now();
        LocalDateTime replanStart = LocalDateTime.of(today, LocalTime.of(10, 0));

        // Machine setup: mill1, lathe1, lathe2
        // Order 1 (due earlier): needs MILLING (takes Ravi, forcing Order 2 onto Kumar in baseline)
        Order order1 = new Order("ORD-HIGH", null, 10, "BLOCK", replanStart.plusDays(1), "OPEN");
        ReflectionTestUtils.setField(order1, "id", 601L);
        Operation op1 = new Operation(order1, 1, "MILLING", 60, "MILLING");
        ReflectionTestUtils.setField(op1, "id", 6001L);

        // Order 2 (due later): needs TURNING
        Order order2 = new Order("ORD-LOW", null, 10, "SHAFT", replanStart.plusDays(2), "OPEN");
        ReflectionTestUtils.setField(order2, "id", 602L);
        Operation op2 = new Operation(order2, 1, "TURNING", 60, "TURNING");
        ReflectionTestUtils.setField(op2, "id", 6002L);

        when(orderRepository.findByStatusIgnoreCase("OPEN")).thenReturn(List.of(order1, order2));
        when(orderRepository.existsById(601L)).thenReturn(true);
        when(orderRepository.existsById(602L)).thenReturn(true);
        when(operationRepository.findAll()).thenReturn(List.of(op1, op2));

        when(machineCapabilityRepository.findByCapabilityIgnoreCase("MILLING"))
                .thenReturn(List.of(new MachineCapability(mill1, "MILLING")));

        when(machineCapabilityRepository.findByCapabilityIgnoreCase("TURNING"))
                .thenReturn(List.of(
                        new MachineCapability(lathe1, "TURNING"),
                        new MachineCapability(lathe2, "TURNING")));

        // Only Ravi has MILLING skill; both Ravi and Kumar have TURNING skill
        when(operatorSkillRepository.findBySkillNameIgnoreCase("MILLING"))
                .thenReturn(List.of(new OperatorSkill(operatorRavi, "MILLING")));

        when(operatorSkillRepository.findBySkillNameIgnoreCase("TURNING"))
                .thenReturn(List.of(
                        new OperatorSkill(operatorRavi, "TURNING"),
                        new OperatorSkill(operatorKumar, "TURNING")));

        OperatorShift shiftRavi = new OperatorShift(operatorRavi, fullDayShift, today, true);
        OperatorShift shiftKumar = new OperatorShift(operatorKumar, fullDayShift, today, true);

        when(operatorShiftRepository.findByOperatorIdAndWorkDateAndAvailableTrue(any(), any()))
                .thenAnswer(inv -> {
                    Long opId = inv.getArgument(0);
                    return opId.equals(1L) ? List.of(shiftRavi) : List.of(shiftKumar);
                });

        // Breakdowns on mill1 (delays Order 1, freeing Ravi at 10:00) AND lathe1 (forces Order 2 to lathe2)
        Breakdown millBreakdown = new Breakdown(mill1, replanStart, replanStart.plusHours(4), "Spindle error");
        Breakdown lathe1Breakdown = new Breakdown(lathe1, replanStart, replanStart.plusHours(4), "Chuck error");
        when(breakdownRepository.findByMachineId(4L)).thenReturn(List.of(millBreakdown));
        when(breakdownRepository.findByMachineId(1L)).thenReturn(List.of(lathe1Breakdown));
        when(breakdownRepository.findByMachineId(2L)).thenReturn(List.of());

        ReplanResultResponse response = schedulerService.replanSchedule(replanStart);

        assertNotNull(response);
        // Both op1 and op2 moved
        assertEquals(2, response.operationsMovedCount());

        // Find op2 delta: was CNC-01 + Kumar in baseline, becomes CNC-02 + Ravi in replan
        OperationScheduleDelta op2Delta = response.impactDeltas().stream()
                .filter(d -> d.orderNumber().equals("ORD-LOW"))
                .findFirst().orElseThrow();

        assertTrue(op2Delta.machineChanged());
        assertTrue(op2Delta.operatorChanged());
        assertEquals("CNC-01", op2Delta.beforeMachineCode());
        assertEquals("CNC-02", op2Delta.afterMachineCode());
        assertEquals("Kumar", op2Delta.beforeOperatorName());
        assertEquals("Ravi", op2Delta.afterOperatorName());
    }

    @Test
    void replanSchedule_zeroImpactBreakdown_producesEmptyDeltas() {
        LocalDate today = LocalDate.now();
        LocalDateTime replanStart = LocalDateTime.of(today, LocalTime.of(10, 0));

        Order order = new Order("ORD-TURNING-ONLY", null, 10, "SHAFT", replanStart.plusDays(2), "OPEN");
        ReflectionTestUtils.setField(order, "id", 700L);

        Operation op = new Operation(order, 1, "TURNING", 60, "TURNING");
        ReflectionTestUtils.setField(op, "id", 7001L);

        when(orderRepository.findByStatusIgnoreCase("OPEN")).thenReturn(List.of(order));
        when(orderRepository.existsById(700L)).thenReturn(true);
        when(operationRepository.findAll()).thenReturn(List.of(op));

        when(machineCapabilityRepository.findByCapabilityIgnoreCase("TURNING"))
                .thenReturn(List.of(new MachineCapability(lathe1, "TURNING")));

        when(operatorSkillRepository.findBySkillNameIgnoreCase("TURNING"))
                .thenReturn(List.of(new OperatorSkill(operatorRavi, "TURNING")));

        OperatorShift shift = new OperatorShift(operatorRavi, fullDayShift, today, true);
        when(operatorShiftRepository.findByOperatorIdAndWorkDateAndAvailableTrue(any(), any()))
                .thenReturn(List.of(shift));

        // Breakdown occurs on MILL-01 (not used by any order)
        Breakdown millBreakdown = new Breakdown(mill1, replanStart, replanStart.plusHours(4), "Hydraulic leak");
        when(breakdownRepository.findByMachineId(4L)).thenReturn(List.of(millBreakdown));
        when(breakdownRepository.findByMachineId(1L)).thenReturn(List.of());

        ReplanResultResponse response = schedulerService.replanSchedule(replanStart);

        assertNotNull(response);
        assertEquals(1, response.totalOperations());
        assertEquals(0, response.operationsMovedCount());
        assertEquals(0, response.machinesReassignedCount());
        assertEquals(0, response.operatorsReassignedCount());
        assertEquals(0, response.ordersDelayedCount());
        assertTrue(response.impactDeltas().isEmpty());
    }

    @Test
    void replanSchedule_transientOperationsWithNullDatabaseIds_matchesReliably() {
        LocalDate today = LocalDate.now();
        LocalDateTime replanStart = LocalDateTime.of(today, LocalTime.of(10, 0));

        // Create transient Order and Operations with NO database IDs (id == null)
        Order transientOrder = new Order("ORD-TRANSIENT", null, 10, "SHAFT", replanStart.plusDays(2), "OPEN");
        Operation transientOp1 = new Operation(transientOrder, 1, "TURNING", 60, "TURNING");
        Operation transientOp2 = new Operation(transientOrder, 2, "TURNING", 60, "TURNING");

        when(orderRepository.findByStatusIgnoreCase("OPEN")).thenReturn(List.of(transientOrder));
        when(operationRepository.findAll()).thenReturn(List.of(transientOp1, transientOp2));

        when(machineCapabilityRepository.findByCapabilityIgnoreCase("TURNING"))
                .thenReturn(List.of(
                        new MachineCapability(lathe1, "TURNING"),
                        new MachineCapability(lathe2, "TURNING")));

        when(operatorSkillRepository.findBySkillNameIgnoreCase("TURNING"))
                .thenReturn(List.of(new OperatorSkill(operatorRavi, "TURNING")));

        OperatorShift shift = new OperatorShift(operatorRavi, fullDayShift, today, true);
        when(operatorShiftRepository.findByOperatorIdAndWorkDateAndAvailableTrue(any(), any()))
                .thenReturn(List.of(shift));

        Breakdown breakdown = new Breakdown(lathe1, replanStart, replanStart.plusHours(4), "Belt replacement");
        when(breakdownRepository.findByMachineId(1L)).thenReturn(List.of(breakdown));
        when(breakdownRepository.findByMachineId(2L)).thenReturn(List.of());

        ReplanResultResponse response = schedulerService.replanSchedule(replanStart);

        assertNotNull(response);
        assertEquals(2, response.totalOperations());
        assertEquals(2, response.operationsMovedCount());
        assertEquals(2, response.machinesReassignedCount());
        assertEquals(2, response.impactDeltas().size());

        OperationScheduleDelta delta1 = response.impactDeltas().get(0);
        OperationScheduleDelta delta2 = response.impactDeltas().get(1);
        assertEquals("ORD-TRANSIENT", delta1.orderNumber());
        assertEquals(1, delta1.sequenceNumber());
        assertEquals("ORD-TRANSIENT", delta2.orderNumber());
        assertEquals(2, delta2.sequenceNumber());
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

    @Test
    void replanSchedule_withSequentialDependencies_shiftsDownstreamOperationsWhenTurningDelayed() {
        LocalDate today = LocalDate.now();
        LocalDateTime replanStart = LocalDateTime.of(today, LocalTime.of(8, 0));

        Order order = new Order("ORD-SEQ-001", null, 50, "SHAFT", replanStart.plusDays(3), "OPEN");
        ReflectionTestUtils.setField(order, "id", 500L);

        // Sequence 1: TURNING on lathe1 (only lathe1 available)
        Operation op1 = new Operation(order, 1, "TURNING", 60, "TURNING");
        ReflectionTestUtils.setField(op1, "id", 5001L);

        // Sequence 2: MILLING on mill1
        Operation op2 = new Operation(order, 2, "MILLING", 60, "MILLING");
        ReflectionTestUtils.setField(op2, "id", 5002L);

        when(orderRepository.findByStatusIgnoreCase("OPEN")).thenReturn(List.of(order));
        when(orderRepository.existsById(500L)).thenReturn(true);
        when(operationRepository.findAll()).thenReturn(List.of(op1, op2));

        when(machineCapabilityRepository.findByCapabilityIgnoreCase("TURNING"))
                .thenReturn(List.of(new MachineCapability(lathe1, "TURNING")));
        when(machineCapabilityRepository.findByCapabilityIgnoreCase("MILLING"))
                .thenReturn(List.of(new MachineCapability(mill1, "MILLING")));

        when(operatorSkillRepository.findBySkillNameIgnoreCase("TURNING"))
                .thenReturn(List.of(new OperatorSkill(operatorRavi, "TURNING")));
        when(operatorSkillRepository.findBySkillNameIgnoreCase("MILLING"))
                .thenReturn(List.of(new OperatorSkill(operatorRavi, "MILLING")));

        OperatorShift shift = new OperatorShift(operatorRavi, fullDayShift, today, true);
        when(operatorShiftRepository.findByOperatorIdAndWorkDateAndAvailableTrue(any(), any()))
                .thenReturn(List.of(shift));

        // Breakdown on lathe1 from 8:00 to 11:00 AM
        Breakdown breakdown = new Breakdown(lathe1, replanStart, replanStart.plusHours(3), "Tooling jammed");
        when(breakdownRepository.findByMachineId(1L)).thenReturn(List.of(breakdown));
        when(breakdownRepository.findByMachineId(4L)).thenReturn(List.of());

        ReplanResultResponse response = schedulerService.replanSchedule(replanStart);

        assertNotNull(response);
        assertEquals(2, response.totalOperations());
        assertEquals(2, response.operationsMovedCount());

        ScheduleResult afterOp1 = response.afterSchedule().get(0);
        ScheduleResult afterOp2 = response.afterSchedule().get(1);

        // Op1 shifted to after breakdown: 11:00 to 12:00
        assertEquals(replanStart.plusHours(3), afterOp1.getStartTime());
        assertEquals(replanStart.plusHours(4), afterOp1.getEndTime());

        // Op2 must start AFTER Op1 completes: at or after 12:00
        assertTrue(!afterOp2.getStartTime().isBefore(afterOp1.getEndTime()));
        assertEquals(afterOp1.getEndTime(), afterOp2.getStartTime());
    }

    @Test
    void replanSchedule_whenBreakdownPrecedesNow_resolvesEarliestBreakdownAsBaselineTime() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDateTime breakdownStart = LocalDateTime.of(yesterday, LocalTime.of(10, 20));
        LocalDateTime breakdownEnd = LocalDateTime.of(yesterday, LocalTime.of(14, 0));

        Order order = new Order("ORD-HISTORIC", null, 10, "SHAFT", breakdownStart.plusDays(3), "OPEN");
        ReflectionTestUtils.setField(order, "id", 600L);

        Operation op = new Operation(order, 1, "TURNING", 60, "TURNING");
        ReflectionTestUtils.setField(op, "id", 6001L);

        when(orderRepository.findByStatusIgnoreCase("OPEN")).thenReturn(List.of(order));
        when(orderRepository.existsById(600L)).thenReturn(true);
        when(operationRepository.findAll()).thenReturn(List.of(op));

        when(machineCapabilityRepository.findByCapabilityIgnoreCase("TURNING"))
                .thenReturn(List.of(
                        new MachineCapability(lathe1, "TURNING"),
                        new MachineCapability(lathe2, "TURNING")));

        when(operatorSkillRepository.findBySkillNameIgnoreCase("TURNING"))
                .thenReturn(List.of(new OperatorSkill(operatorRavi, "TURNING")));

        OperatorShift shiftYesterday = new OperatorShift(operatorRavi, fullDayShift, yesterday, true);
        OperatorShift shiftToday = new OperatorShift(operatorRavi, fullDayShift, LocalDate.now(), true);
        when(operatorShiftRepository.findByOperatorIdAndWorkDateAndAvailableTrue(any(), any()))
                .thenReturn(List.of(shiftYesterday, shiftToday));

        Breakdown breakdown = new Breakdown(lathe1, breakdownStart, breakdownEnd, "Spindle repair");
        when(breakdownRepository.findAll()).thenReturn(List.of(breakdown));
        when(breakdownRepository.findByMachineId(1L)).thenReturn(List.of(breakdown));
        when(breakdownRepository.findByMachineId(2L)).thenReturn(List.of());

        // Call replan with null parameters (simulating frontend call)
        ReplanResultResponse response = schedulerService.replanSchedule(null);

        assertNotNull(response);
        assertEquals(1, response.totalOperations());
        assertEquals(1, response.operationsMovedCount());
        assertEquals(1, response.machinesReassignedCount());
        assertEquals("CNC-02", response.afterSchedule().get(0).getMachine().getMachineCode());
    }

    @Test
    void replanSchedule_minimalDisruption_unrelatedOrdersRemainUntouched() {
        LocalDate today = LocalDate.now();
        LocalDateTime baseTime = LocalDateTime.of(today, LocalTime.of(18, 0));
        LocalDateTime replanStart = LocalDateTime.of(today, LocalTime.of(18, 45));

        // Order 1 on CNC-01 (conflicted)
        Order order1 = new Order("ORD-001", null, 10, "SHAFT", baseTime.plusDays(3), "OPEN");
        ReflectionTestUtils.setField(order1, "id", 701L);
        Operation op1_turning = new Operation(order1, 1, "TURNING", 90, "TURNING");
        ReflectionTestUtils.setField(op1_turning, "id", 7001L);

        // Order 2 on CNC-02 (unaffected)
        Order order2 = new Order("ORD-002", null, 10, "SHAFT", baseTime.plusDays(3), "OPEN");
        ReflectionTestUtils.setField(order2, "id", 702L);
        Operation op2_turning = new Operation(order2, 1, "TURNING", 90, "TURNING");
        ReflectionTestUtils.setField(op2_turning, "id", 7002L);

        // Order 3 on GRIND-01 (unaffected)
        Order order3 = new Order("ORD-003", null, 10, "GEAR", baseTime.plusDays(3), "OPEN");
        ReflectionTestUtils.setField(order3, "id", 703L);
        Operation op3_grind = new Operation(order3, 1, "GRINDING", 60, "GRINDING");
        ReflectionTestUtils.setField(op3_grind, "id", 7003L);

        when(orderRepository.findByStatusIgnoreCase("OPEN")).thenReturn(List.of(order1, order2, order3));
        when(orderRepository.existsById(701L)).thenReturn(true);
        when(orderRepository.existsById(702L)).thenReturn(true);
        when(orderRepository.existsById(703L)).thenReturn(true);
        when(operationRepository.findAll()).thenReturn(List.of(op1_turning, op2_turning, op3_grind));

        when(machineCapabilityRepository.findByCapabilityIgnoreCase("TURNING"))
                .thenReturn(List.of(
                        new MachineCapability(lathe1, "TURNING"),
                        new MachineCapability(lathe2, "TURNING")));
        when(machineCapabilityRepository.findByCapabilityIgnoreCase("GRINDING"))
                .thenReturn(List.of(new MachineCapability(grinder1, "GRINDING")));

        when(operatorSkillRepository.findBySkillNameIgnoreCase("TURNING"))
                .thenReturn(List.of(new OperatorSkill(operatorRavi, "TURNING"), new OperatorSkill(operatorKumar, "TURNING")));
        when(operatorSkillRepository.findBySkillNameIgnoreCase("GRINDING"))
                .thenReturn(List.of(new OperatorSkill(operatorKumar, "GRINDING")));

        OperatorShift shiftRavi = new OperatorShift(operatorRavi, fullDayShift, today, true);
        OperatorShift shiftKumar = new OperatorShift(operatorKumar, fullDayShift, today, true);
        when(operatorShiftRepository.findByOperatorIdAndWorkDateAndAvailableTrue(any(), any()))
                .thenReturn(List.of(shiftRavi, shiftKumar));

        // CNC-01 breakdown from 18:45 to 20:00 (overlaps op1_turning)
        Breakdown breakdownCNC1 = new Breakdown(lathe1, replanStart, replanStart.plusMinutes(75), "Tooling jam");
        when(breakdownRepository.findAll()).thenReturn(List.of(breakdownCNC1));
        when(breakdownRepository.findByMachineId(1L)).thenReturn(List.of(breakdownCNC1));
        when(breakdownRepository.findByMachineId(2L)).thenReturn(List.of());
        when(breakdownRepository.findByMachineId(3L)).thenReturn(List.of());

        ReplanResultResponse response = schedulerService.replanSchedule(baseTime, replanStart);

        assertNotNull(response);
        assertEquals(3, response.totalOperations());
        // Only ORD-001 is moved; ORD-002 and ORD-003 remain completely unchanged!
        assertEquals(1, response.operationsMovedCount(), "Only directly affected ORD-001 should move");

        ScheduleResult beforeOp2 = response.beforeSchedule().stream()
                .filter(r -> r.getOperation().getId().equals(7002L))
                .findFirst().orElseThrow();
        ScheduleResult afterOp2 = response.afterSchedule().stream()
                .filter(r -> r.getOperation().getId().equals(7002L))
                .findFirst().orElseThrow();

        ScheduleResult beforeOp3 = response.beforeSchedule().stream()
                .filter(r -> r.getOperation().getId().equals(7003L))
                .findFirst().orElseThrow();
        ScheduleResult afterOp3 = response.afterSchedule().stream()
                .filter(r -> r.getOperation().getId().equals(7003L))
                .findFirst().orElseThrow();

        assertEquals(beforeOp2.getStartTime(), afterOp2.getStartTime());
        assertEquals(beforeOp3.getStartTime(), afterOp3.getStartTime());
        assertEquals(beforeOp3.getMachine().getMachineCode(), afterOp3.getMachine().getMachineCode());
    }
}
