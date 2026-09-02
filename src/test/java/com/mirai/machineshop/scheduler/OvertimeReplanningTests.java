package com.mirai.machineshop.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import com.mirai.machineshop.dto.ReplanResultResponse;
import com.mirai.machineshop.entity.Breakdown;
import com.mirai.machineshop.entity.Customer;
import com.mirai.machineshop.entity.Machine;
import com.mirai.machineshop.entity.MachineCapability;
import com.mirai.machineshop.entity.Operation;
import com.mirai.machineshop.entity.Operator;
import com.mirai.machineshop.entity.OperatorAbsence;
import com.mirai.machineshop.entity.OperatorShift;
import com.mirai.machineshop.entity.OperatorSkill;
import com.mirai.machineshop.entity.Order;
import com.mirai.machineshop.entity.Shift;
import com.mirai.machineshop.repository.BreakdownRepository;
import com.mirai.machineshop.repository.ChangeoverRepository;
import com.mirai.machineshop.repository.MachineCapabilityRepository;
import com.mirai.machineshop.repository.MaterialDelayRepository;
import com.mirai.machineshop.repository.OperationRepository;
import com.mirai.machineshop.repository.OperatorAbsenceRepository;
import com.mirai.machineshop.repository.OperatorShiftRepository;
import com.mirai.machineshop.repository.OperatorSkillRepository;
import com.mirai.machineshop.repository.OrderRepository;
import com.mirai.machineshop.service.CostCalculationService;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class OvertimeReplanningTests {

    @Mock
    private MachineCapabilityRepository machineCapabilityRepository;

    @Mock
    private OperationRepository operationRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OperatorSkillRepository operatorSkillRepository;

    @Mock
    private OperatorShiftRepository operatorShiftRepository;

    @Mock
    private ChangeoverRepository changeoverRepository;

    @Mock
    private BreakdownRepository breakdownRepository;

    @Mock
    private OperatorAbsenceRepository operatorAbsenceRepository;

    @Mock
    private MaterialDelayRepository materialDelayRepository;

    private CostCalculationService costCalculationService;
    private SchedulerService schedulerService;

    private Machine cnc1;
    private Operator operatorRavi;
    private Shift shift1;
    private Shift shift2;
    private Customer tier1Cust;

    @BeforeEach
    void setUp() {
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
                operatorAbsenceRepository,
                materialDelayRepository,
                costCalculationService
        );

        cnc1 = new Machine("CNC-01", "CNC Lathe 1", "TURNING");
        ReflectionTestUtils.setField(cnc1, "id", 1L);

        operatorRavi = new Operator("OP-001", "Ravi");
        ReflectionTestUtils.setField(operatorRavi, "id", 10L);

        shift1 = new Shift("SHIFT-1", LocalTime.of(6, 0), LocalTime.of(14, 0));
        shift2 = new Shift("SHIFT-2", LocalTime.of(14, 0), LocalTime.of(22, 0));

        tier1Cust = new Customer("CUST-1", "Apex Auto Systems", "TIER-1");
        ReflectionTestUtils.setField(tier1Cust, "id", 100L);

        when(changeoverRepository.findAll()).thenReturn(List.of());
        when(operatorAbsenceRepository.findAll()).thenReturn(List.of());
        when(materialDelayRepository.findAll()).thenReturn(List.of());
    }

    @Test
    void replanSchedule_choosesOvertime_whenLatePenaltyIsMateriallyHigher() {
        LocalDate today = LocalDate.now();
        LocalDateTime baseTime = LocalDateTime.of(today, LocalTime.of(6, 0));
        // Due date: Today at 15:30 (Tier 1 customer => ₹150/hr late penalty)
        LocalDateTime dueDate = LocalDateTime.of(today, LocalTime.of(15, 30));

        Machine mill1 = new Machine("MILL-01", "Milling Machine 1", "MILLING");
        ReflectionTestUtils.setField(mill1, "id", 2L);

        Operator operatorKumar = new Operator("OP-002", "Kumar");
        ReflectionTestUtils.setField(operatorKumar, "id", 20L);

        // Order 1 is Turning 60 min, due today at 15:30 (Tier 1 customer)
        Order order1 = new Order("ORD-001", tier1Cust, 10, "SHAFT", dueDate, "OPEN");
        ReflectionTestUtils.setField(order1, "id", 1L);
        Operation op1 = new Operation(order1, 1, "TURNING", 60, "TURNING");
        ReflectionTestUtils.setField(op1, "id", 11L);

        // Order 2 is Milling 480 min on MILL-01 with Ravi (06:00 -> 14:00)
        Order order2 = new Order("ORD-002", tier1Cust, 10, "SHAFT", dueDate.plusDays(5), "OPEN");
        ReflectionTestUtils.setField(order2, "id", 2L);
        Operation op2 = new Operation(order2, 1, "MILLING", 480, "MILLING");
        ReflectionTestUtils.setField(op2, "id", 21L);

        MachineCapability capCnc = new MachineCapability(cnc1, "TURNING");
        MachineCapability capMill = new MachineCapability(mill1, "MILLING");
        when(machineCapabilityRepository.findAll()).thenReturn(List.of(capCnc, capMill));

        // Kumar is primary for TURNING; Ravi is for MILLING and also has TURNING
        OperatorSkill skillKumarTurning = new OperatorSkill(operatorKumar, "TURNING");
        OperatorSkill skillRaviMilling = new OperatorSkill(operatorRavi, "MILLING");
        OperatorSkill skillRaviTurning = new OperatorSkill(operatorRavi, "TURNING");
        when(operatorSkillRepository.findAll()).thenReturn(List.of(skillKumarTurning, skillRaviMilling, skillRaviTurning));

        // Ravi and Kumar on Shift 1 today and tomorrow
        OperatorShift shiftRaviToday = new OperatorShift(operatorRavi, shift1, today, true);
        OperatorShift shiftRaviTomorrow = new OperatorShift(operatorRavi, shift1, today.plusDays(1), true);
        OperatorShift shiftKumarToday = new OperatorShift(operatorKumar, shift1, today, true);
        OperatorShift shiftKumarTomorrow = new OperatorShift(operatorKumar, shift1, today.plusDays(1), true);
        when(operatorShiftRepository.findAll()).thenReturn(List.of(shiftRaviToday, shiftRaviTomorrow, shiftKumarToday, shiftKumarTomorrow));

        when(orderRepository.findByStatusIgnoreCase("OPEN")).thenReturn(List.of(order2, order1));
        when(operationRepository.findAll()).thenReturn(List.of(op2, op1));

        // Baseline: No disruptions
        when(breakdownRepository.findAll()).thenReturn(List.of());
        when(operatorAbsenceRepository.findAll()).thenReturn(List.of());

        // Disruption: Kumar is absent for 24 hours
        OperatorAbsence absence = new OperatorAbsence(operatorKumar, baseTime, baseTime.plusHours(24), "Sick leave");
        ReflectionTestUtils.setField(absence, "id", 301L);
        when(operatorAbsenceRepository.findAll()).thenReturn(List.of(absence));
        when(operatorAbsenceRepository.findByOperatorId(20L)).thenReturn(List.of(absence));
        when(operatorAbsenceRepository.findByOperatorId(10L)).thenReturn(List.of());

        ReplanResultResponse result = schedulerService.replanSchedule();

        assertNotNull(result);
        assertEquals(2, result.totalOperations());
        assertEquals(1, result.operationsMovedCount()); // Only op1 moved

        ScheduleResult afterOp1 = result.afterSchedule().stream()
                .filter(r -> r.getOperation().getId().equals(11L))
                .findFirst().orElseThrow();

        // Verification A: Op 1 was scheduled on Overtime with Ravi from 14:00 to 15:00 (after Ravi finishes Op 2 at 14:00)
        assertEquals(LocalDateTime.of(today, LocalTime.of(14, 0)), afterOp1.getStartTime());
        assertEquals(LocalDateTime.of(today, LocalTime.of(15, 0)), afterOp1.getEndTime());
        assertEquals("CNC-01", afterOp1.getMachine().getMachineCode());
        assertEquals("OP-001", afterOp1.getOperator().getOperatorCode());

        // Verification B: Overtime was generated and calculated correctly (Ravi: 480 min on op2 + 60 min on op1 = 540 min = 1 hr OT)
        assertNotNull(result.afterCostSummary());
        assertEquals(1.0, result.afterCostSummary().totalOvertimeHours());
        assertEquals(500.0, result.afterCostSummary().totalOvertimeCost());

        // Verification C: Late penalty remained 0 (due date 15:30 was met)
        assertEquals(0, result.afterCostSummary().lateOrdersCount());
        assertEquals(0.0, result.afterCostSummary().totalPenaltyCost());

        // Verification D: Supervisor-friendly Overtime vs Late Penalty Comparison DTO
        assertNotNull(result.overtimeComparison());
        assertEquals("OVERTIME_SELECTED", result.overtimeComparison().decisionStatus());
        assertEquals("ORD-001", result.overtimeComparison().orderNumber());
        assertEquals("TURNING", result.overtimeComparison().operationType());
        assertEquals(500.0, result.overtimeComparison().overtimeLaborCost());
        assertTrue(result.overtimeComparison().regularRecoveryCost() > result.overtimeComparison().overtimeRecoveryCost());
        assertTrue(result.overtimeComparison().savings() > 0);
        assertTrue(result.overtimeComparison().recommendation().contains("Overtime chosen"));
    }

    @Test
    void replanSchedule_rejectsOvertime_whenRegularNextShiftHasZeroPenalty() {
        LocalDate today = LocalDate.now();
        LocalDateTime baseTime = LocalDateTime.of(today, LocalTime.of(6, 0));
        // Due date: 5 days later (no risk of late penalty)
        LocalDateTime dueDate = baseTime.plusDays(5);

        Order order1 = new Order("ORD-001", tier1Cust, 10, "SHAFT", dueDate, "OPEN");
        ReflectionTestUtils.setField(order1, "id", 1L);

        Operation op1 = new Operation(order1, 1, "TURNING", 60, "TURNING");
        ReflectionTestUtils.setField(op1, "id", 11L);

        MachineCapability cap = new MachineCapability(cnc1, "TURNING");
        when(machineCapabilityRepository.findAll()).thenReturn(List.of(cap));

        OperatorSkill skill = new OperatorSkill(operatorRavi, "TURNING");
        when(operatorSkillRepository.findAll()).thenReturn(List.of(skill));

        OperatorShift shiftToday = new OperatorShift(operatorRavi, shift1, today, true);
        OperatorShift shiftTomorrow = new OperatorShift(operatorRavi, shift1, today.plusDays(1), true);
        when(operatorShiftRepository.findAll()).thenReturn(List.of(shiftToday, shiftTomorrow));

        when(orderRepository.findByStatusIgnoreCase("OPEN")).thenReturn(List.of(order1));
        when(operationRepository.findAll()).thenReturn(List.of(op1));

        // Baseline: No breakdown
        when(breakdownRepository.findAll()).thenReturn(List.of());

        // Disruption: CNC-01 breaks down during entire Shift 1 today (06:00 -> 14:00)
        Breakdown breakdown = new Breakdown(cnc1, baseTime, baseTime.plusHours(8), "Spindle failure");
        ReflectionTestUtils.setField(breakdown, "id", 201L);
        when(breakdownRepository.findAll()).thenReturn(List.of(breakdown));
        when(breakdownRepository.findByMachineId(1L)).thenReturn(List.of(breakdown));

        ReplanResultResponse result = schedulerService.replanSchedule();

        assertNotNull(result);
        assertEquals(1, result.totalOperations());

        ScheduleResult afterOp = result.afterSchedule().get(0);

        // Verification A: The operation was scheduled on tomorrow's regular shift at 06:00 AM (NOT overtime today)
        assertEquals(LocalDateTime.of(today.plusDays(1), LocalTime.of(6, 0)), afterOp.getStartTime());
        assertEquals(LocalDateTime.of(today.plusDays(1), LocalTime.of(7, 0)), afterOp.getEndTime());

        // Verification B: Overtime is 0
        assertEquals(0.0, result.afterCostSummary().totalOvertimeHours());
        assertEquals(0.0, result.afterCostSummary().totalOvertimeCost());
        assertEquals(0.0, result.afterCostSummary().totalPenaltyCost());
        assertEquals(0.0, result.afterCostSummary().totalCost());

        // Verification C: Supervisor-friendly Overtime vs Late Penalty Comparison DTO
        assertNotNull(result.overtimeComparison());
        assertEquals("OVERTIME_REJECTED", result.overtimeComparison().decisionStatus());
        assertEquals("ORD-001", result.overtimeComparison().orderNumber());
        assertEquals("TURNING", result.overtimeComparison().operationType());
        assertEquals(0.0, result.overtimeComparison().regularRecoveryCost());
        assertEquals(500.0, result.overtimeComparison().overtimeLaborCost());
        assertEquals(0.0, result.overtimeComparison().savings());
        assertTrue(result.overtimeComparison().recommendation().contains("Regular shift chosen"));
    }
}
