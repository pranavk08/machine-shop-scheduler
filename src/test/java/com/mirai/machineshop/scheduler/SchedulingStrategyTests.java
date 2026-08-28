package com.mirai.machineshop.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.mirai.machineshop.dto.StrategyComparisonResponse;
import com.mirai.machineshop.dto.StrategyEvaluationResult;
import com.mirai.machineshop.entity.Customer;
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
import com.mirai.machineshop.service.CostCalculationService;

class SchedulingStrategyTests {

    private MachineCapabilityRepository machineCapabilityRepository;
    private OperationRepository operationRepository;
    private OrderRepository orderRepository;
    private OperatorSkillRepository operatorSkillRepository;
    private OperatorShiftRepository operatorShiftRepository;
    private ChangeoverRepository changeoverRepository;
    private BreakdownRepository breakdownRepository;
    private CostCalculationService costCalculationService;
    private SchedulerService schedulerService;

    private Customer tier1Customer;
    private Customer tier2Customer;
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

        costCalculationService = new CostCalculationService(
                changeoverRepository,
                480,
                500.0,
                150.0,
                75.0,
                300.0
        );

        schedulerService = new SchedulerService(
                machineCapabilityRepository,
                operationRepository,
                orderRepository,
                operatorSkillRepository,
                operatorShiftRepository,
                changeoverRepository,
                breakdownRepository,
                costCalculationService
        );

        tier1Customer = new Customer("CUST-001", "Apex Auto Systems", "TIER-1");
        ReflectionTestUtils.setField(tier1Customer, "id", 1L);

        tier2Customer = new Customer("CUST-003", "Delta Components", "TIER-2");
        ReflectionTestUtils.setField(tier2Customer, "id", 2L);

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
        when(breakdownRepository.findByMachineId(any())).thenReturn(List.of());

        OperatorShift shiftRavi = new OperatorShift(operatorRavi, fullDayShift, LocalDate.now(), true);
        OperatorShift shiftKumar = new OperatorShift(operatorKumar, fullDayShift, LocalDate.now(), true);
        when(operatorShiftRepository.findByOperatorIdAndWorkDateAndAvailableTrue(any(), any()))
                .thenAnswer(inv -> {
                    Long opId = inv.getArgument(0);
                    return opId.equals(1L) ? List.of(shiftRavi) : List.of(shiftKumar);
                });
    }

    @Test
    void scheduleAllOpenOrders_mostOnTimeStrategy_prioritizesTier1Customer() {
        LocalDateTime now = LocalDateTime.now();

        // Order 1: Tier-2 Customer, Due Day 1 (earlier deadline)
        Order orderTier2 = new Order("ORD-TIER2", tier2Customer, 10, "SHAFT", now.plusDays(1), "OPEN");
        ReflectionTestUtils.setField(orderTier2, "id", 101L);
        Operation opTier2 = new Operation(orderTier2, 1, "TURNING", 60, "TURNING");
        ReflectionTestUtils.setField(opTier2, "id", 1001L);

        // Order 2: Tier-1 Customer, Due Day 2 (later deadline)
        Order orderTier1 = new Order("ORD-TIER1", tier1Customer, 10, "SHAFT", now.plusDays(2), "OPEN");
        ReflectionTestUtils.setField(orderTier1, "id", 102L);
        Operation opTier1 = new Operation(orderTier1, 1, "TURNING", 60, "TURNING");
        ReflectionTestUtils.setField(opTier1, "id", 1002L);

        when(orderRepository.findByStatusIgnoreCase("OPEN")).thenReturn(List.of(orderTier2, orderTier1));
        when(operationRepository.findAll()).thenReturn(List.of(opTier2, opTier1));
        when(machineCapabilityRepository.findByCapabilityIgnoreCase("TURNING"))
                .thenReturn(List.of(new MachineCapability(lathe1, "TURNING")));
        when(operatorSkillRepository.findBySkillNameIgnoreCase("TURNING"))
                .thenReturn(List.of(new OperatorSkill(operatorRavi, "TURNING")));

        List<ScheduleResult> results = schedulerService.scheduleAllOpenOrders(SchedulingStrategy.MOST_ON_TIME);

        assertNotNull(results);
        assertEquals(2, results.size());

        // Under MOST_ON_TIME, Tier-1 customer order is scheduled first at now
        assertEquals("ORD-TIER1", results.get(0).getOperation().getOrder().getOrderNumber());
        assertEquals("ORD-TIER2", results.get(1).getOperation().getOrder().getOrderNumber());
    }

    @Test
    void scheduleAllOpenOrders_cheapestProductionStrategy_groupsCompatiblePartFamilies() {
        LocalDateTime now = LocalDateTime.now();

        // Order 1: SHAFT, Due Day 3
        Order orderShaft1 = new Order("ORD-SHAFT-1", tier2Customer, 10, "SHAFT", now.plusDays(3), "OPEN");
        ReflectionTestUtils.setField(orderShaft1, "id", 201L);
        Operation opShaft1 = new Operation(orderShaft1, 1, "TURNING", 60, "TURNING");
        ReflectionTestUtils.setField(opShaft1, "id", 2001L);

        // Order 2: GEAR, Due Day 1
        Order orderGear = new Order("ORD-GEAR-1", tier2Customer, 10, "GEAR", now.plusDays(1), "OPEN");
        ReflectionTestUtils.setField(orderGear, "id", 202L);
        Operation opGear = new Operation(orderGear, 1, "TURNING", 60, "TURNING");
        ReflectionTestUtils.setField(opGear, "id", 2002L);

        // Order 3: SHAFT, Due Day 2
        Order orderShaft2 = new Order("ORD-SHAFT-2", tier2Customer, 10, "SHAFT", now.plusDays(2), "OPEN");
        ReflectionTestUtils.setField(orderShaft2, "id", 203L);
        Operation opShaft2 = new Operation(orderShaft2, 1, "TURNING", 60, "TURNING");
        ReflectionTestUtils.setField(opShaft2, "id", 2003L);

        when(orderRepository.findByStatusIgnoreCase("OPEN")).thenReturn(List.of(orderShaft1, orderGear, orderShaft2));
        when(operationRepository.findAll()).thenReturn(List.of(opShaft1, opGear, opShaft2));
        when(machineCapabilityRepository.findByCapabilityIgnoreCase("TURNING"))
                .thenReturn(List.of(new MachineCapability(lathe1, "TURNING")));
        when(operatorSkillRepository.findBySkillNameIgnoreCase("TURNING"))
                .thenReturn(List.of(new OperatorSkill(operatorRavi, "TURNING")));

        List<ScheduleResult> results = schedulerService.scheduleAllOpenOrders(SchedulingStrategy.CHEAPEST_PRODUCTION);

        assertNotNull(results);
        assertEquals(3, results.size());

        // Under CHEAPEST_PRODUCTION, GEAR is clustered first (alphabetically GEAR then SHAFT), and both SHAFT orders are batched together
        assertEquals("GEAR", results.get(0).getOperation().getOrder().getPartFamily());
        assertEquals("SHAFT", results.get(1).getOperation().getOrder().getPartFamily());
        assertEquals("SHAFT", results.get(2).getOperation().getOrder().getPartFamily());
        assertEquals("ORD-SHAFT-2", results.get(1).getOperation().getOrder().getOrderNumber()); // due day 2 before day 3
        assertEquals("ORD-SHAFT-1", results.get(2).getOperation().getOrder().getOrderNumber());
    }

    @Test
    void scheduleAllOpenOrders_mostRobustStrategy_schedulesBottleneckGrinderFirst() {
        LocalDateTime now = LocalDateTime.now();

        // Order 1: Non-bottleneck order (TURNING only), Due Day 2
        Order orderTurningOnly = new Order("ORD-LATHE", tier2Customer, 10, "SHAFT", now.plusDays(2), "OPEN");
        ReflectionTestUtils.setField(orderTurningOnly, "id", 301L);
        Operation opLathe = new Operation(orderTurningOnly, 1, "TURNING", 60, "TURNING");
        ReflectionTestUtils.setField(opLathe, "id", 3001L);

        // Order 2: Bottleneck order (requires GRINDING), Due Day 3
        Order orderGrinding = new Order("ORD-GRIND", tier2Customer, 10, "SHAFT", now.plusDays(3), "OPEN");
        ReflectionTestUtils.setField(orderGrinding, "id", 302L);
        Operation opGrind = new Operation(orderGrinding, 1, "GRINDING", 60, "GRINDING");
        ReflectionTestUtils.setField(opGrind, "id", 3002L);

        when(orderRepository.findByStatusIgnoreCase("OPEN")).thenReturn(List.of(orderTurningOnly, orderGrinding));
        when(operationRepository.findAll()).thenReturn(List.of(opLathe, opGrind));
        when(machineCapabilityRepository.findByCapabilityIgnoreCase("TURNING"))
                .thenReturn(List.of(new MachineCapability(lathe1, "TURNING")));
        when(machineCapabilityRepository.findByCapabilityIgnoreCase("GRINDING"))
                .thenReturn(List.of(new MachineCapability(grinder1, "GRINDING")));
        when(operatorSkillRepository.findBySkillNameIgnoreCase("TURNING"))
                .thenReturn(List.of(new OperatorSkill(operatorRavi, "TURNING")));
        when(operatorSkillRepository.findBySkillNameIgnoreCase("GRINDING"))
                .thenReturn(List.of(new OperatorSkill(operatorKumar, "GRINDING")));

        List<ScheduleResult> results = schedulerService.scheduleAllOpenOrders(SchedulingStrategy.MOST_ROBUST);

        assertNotNull(results);
        assertEquals(2, results.size());

        // Under MOST_ROBUST, order containing GRINDING bottleneck operation is prioritized first
        assertEquals("ORD-GRIND", results.get(0).getOperation().getOrder().getOrderNumber());
        assertEquals("ORD-LATHE", results.get(1).getOperation().getOrder().getOrderNumber());
    }

    @Test
    void compareStrategies_evaluatesAllThreeStrategiesAndRecommendsBest() {
        LocalDateTime now = LocalDateTime.now();

        Order order1 = new Order("ORD-001", tier1Customer, 10, "SHAFT", now.plusDays(2), "OPEN");
        ReflectionTestUtils.setField(order1, "id", 401L);
        Operation op1 = new Operation(order1, 1, "TURNING", 60, "TURNING");
        ReflectionTestUtils.setField(op1, "id", 4001L);

        Order order2 = new Order("ORD-002", tier2Customer, 10, "GEAR", now.plusDays(1), "OPEN");
        ReflectionTestUtils.setField(order2, "id", 402L);
        Operation op2 = new Operation(order2, 1, "TURNING", 60, "TURNING");
        ReflectionTestUtils.setField(op2, "id", 4002L);

        when(orderRepository.findByStatusIgnoreCase("OPEN")).thenReturn(List.of(order1, order2));
        when(operationRepository.findAll()).thenReturn(List.of(op1, op2));
        when(machineCapabilityRepository.findByCapabilityIgnoreCase("TURNING"))
                .thenReturn(List.of(new MachineCapability(lathe1, "TURNING")));
        when(operatorSkillRepository.findBySkillNameIgnoreCase("TURNING"))
                .thenReturn(List.of(new OperatorSkill(operatorRavi, "TURNING")));

        StrategyComparisonResponse response = schedulerService.compareStrategies();

        assertNotNull(response);
        assertEquals(3, response.strategies().size());

        // Check that all 3 strategy names exist in the comparison
        List<String> strategyNames = response.strategies().stream()
                .map(StrategyEvaluationResult::strategy)
                .toList();
        assertTrue(strategyNames.contains("MOST_ON_TIME"));
        assertTrue(strategyNames.contains("CHEAPEST_PRODUCTION"));
        assertTrue(strategyNames.contains("MOST_ROBUST"));

        assertNotNull(response.recommendedStrategy());
        assertNotNull(response.recommendationReason());
        assertTrue(response.recommendationReason().contains("recommended"));
    }

    @Test
    void defaultEndpoint_scheduleAllOpenOrders_backwardCompatible() {
        LocalDateTime now = LocalDateTime.now();

        Order order = new Order("ORD-DEF", tier1Customer, 10, "SHAFT", now.plusDays(2), "OPEN");
        ReflectionTestUtils.setField(order, "id", 501L);
        Operation op = new Operation(order, 1, "TURNING", 60, "TURNING");
        ReflectionTestUtils.setField(op, "id", 5001L);

        when(orderRepository.findByStatusIgnoreCase("OPEN")).thenReturn(List.of(order));
        when(operationRepository.findAll()).thenReturn(List.of(op));
        when(machineCapabilityRepository.findByCapabilityIgnoreCase("TURNING"))
                .thenReturn(List.of(new MachineCapability(lathe1, "TURNING")));
        when(operatorSkillRepository.findBySkillNameIgnoreCase("TURNING"))
                .thenReturn(List.of(new OperatorSkill(operatorRavi, "TURNING")));

        // Calling no-arg method (used by default GET /api/scheduler/orders/schedule)
        List<ScheduleResult> results = schedulerService.scheduleAllOpenOrders();

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("ORD-DEF", results.get(0).getOperation().getOrder().getOrderNumber());
    }
}
