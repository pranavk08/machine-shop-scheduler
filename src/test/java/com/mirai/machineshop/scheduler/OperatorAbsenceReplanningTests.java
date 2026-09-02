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

import com.mirai.machineshop.dto.OperationScheduleDelta;
import com.mirai.machineshop.dto.ReplanResultResponse;
import com.mirai.machineshop.entity.Breakdown;
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
import com.mirai.machineshop.repository.OperationRepository;
import com.mirai.machineshop.repository.OperatorAbsenceRepository;
import com.mirai.machineshop.repository.OperatorShiftRepository;
import com.mirai.machineshop.repository.OperatorSkillRepository;
import com.mirai.machineshop.repository.OrderRepository;
import com.mirai.machineshop.service.CostCalculationService;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class OperatorAbsenceReplanningTests {

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

    private SchedulerService schedulerService;

    private Machine grind1;
    private Operator operatorKumar; // Grinder operator 1
    private Operator operatorAnita; // Grinder operator 2
    private Shift shift1;
    private Shift shift2;

    @BeforeEach
    void setUp() {
        CostCalculationService costCalculationService =
                new CostCalculationService(changeoverRepository, 480, 500.0, 150.0, 75.0, 300.0);

        schedulerService = new SchedulerService(
                machineCapabilityRepository,
                operationRepository,
                orderRepository,
                operatorSkillRepository,
                operatorShiftRepository,
                changeoverRepository,
                breakdownRepository,
                operatorAbsenceRepository,
                costCalculationService);

        grind1 = new Machine("GRIND-01", "Surface Grinder 1", "GRINDING");
        ReflectionTestUtils.setField(grind1, "id", 501L);

        operatorKumar = new Operator("OP-002", "Kumar");
        ReflectionTestUtils.setField(operatorKumar, "id", 201L);

        operatorAnita = new Operator("OP-003", "Anita");
        ReflectionTestUtils.setField(operatorAnita, "id", 202L);

        shift1 = new Shift("Shift 1", LocalTime.of(6, 0), LocalTime.of(14, 0));
        ReflectionTestUtils.setField(shift1, "id", 1L);

        shift2 = new Shift("Shift 2", LocalTime.of(14, 0), LocalTime.of(22, 0));
        ReflectionTestUtils.setField(shift2, "id", 2L);
    }

    @Test
    void replanSchedule_operatorAbsent_replacementAvailable_reassignsOperatorOnSameMachineAndSlot() {
        LocalDate today = LocalDate.now();
        LocalDateTime baseTime = LocalDateTime.of(today, LocalTime.of(6, 0));

        Order order1 = new Order("ORD-001", null, 10, "SHAFT", baseTime.plusDays(3), "OPEN");
        ReflectionTestUtils.setField(order1, "id", 101L);
        Operation op1 = new Operation(order1, 1, "GRINDING", 60, "GRINDING");
        ReflectionTestUtils.setField(op1, "id", 1001L);

        when(orderRepository.findByStatusIgnoreCase("OPEN")).thenReturn(List.of(order1));
        when(orderRepository.existsById(101L)).thenReturn(true);
        when(operationRepository.findAll()).thenReturn(List.of(op1));

        when(machineCapabilityRepository.findByCapabilityIgnoreCase("GRINDING"))
                .thenReturn(List.of(new MachineCapability(grind1, "GRINDING")));

        when(operatorSkillRepository.findBySkillNameIgnoreCase("GRINDING"))
                .thenReturn(List.of(
                        new OperatorSkill(operatorKumar, "GRINDING"),
                        new OperatorSkill(operatorAnita, "GRINDING")));

        // Both Kumar and Anita work Shift 1 on today
        OperatorShift shiftKumar = new OperatorShift(operatorKumar, shift1, today, true);
        OperatorShift shiftAnita = new OperatorShift(operatorAnita, shift1, today, true);

        when(operatorShiftRepository.findByOperatorIdAndWorkDateAndAvailableTrue(any(), any()))
                .thenAnswer(inv -> {
                    Long opId = inv.getArgument(0);
                    LocalDate d = inv.getArgument(1);
                    if (today.equals(d)) {
                        if (Long.valueOf(201L).equals(opId)) return List.of(shiftKumar);
                        if (Long.valueOf(202L).equals(opId)) return List.of(shiftAnita);
                    }
                    return List.of();
                });

        when(breakdownRepository.findAll()).thenReturn(List.of());

        // Kumar is absent today from 06:00 to 14:00 (medical leave)
        LocalDateTime absenceStart = LocalDateTime.of(today, LocalTime.of(6, 0));
        LocalDateTime absenceEnd = LocalDateTime.of(today, LocalTime.of(14, 0));
        OperatorAbsence kumarAbsence = new OperatorAbsence(operatorKumar, absenceStart, absenceEnd, "Medical Leave");

        when(operatorAbsenceRepository.findAll()).thenReturn(List.of(kumarAbsence));
        when(operatorAbsenceRepository.findByOperatorId(201L)).thenReturn(List.of(kumarAbsence));
        when(operatorAbsenceRepository.findByOperatorId(202L)).thenReturn(List.of());

        // Baseline schedule had Kumar assigned (since Before schedule is disruption-unaware)
        // When replanned, Kumar's absence is detected. Anita is qualified and available.
        ReplanResultResponse response = schedulerService.replanSchedule(SchedulingStrategy.MOST_ON_TIME, baseTime, baseTime);

        assertNotNull(response);
        assertEquals(1, response.operationsMovedCount(), "Exactly 1 operation should be modified");
        assertEquals(0, response.machinesReassignedCount(), "Machine must remain GRIND-01");
        assertEquals(1, response.operatorsReassignedCount(), "Operator should be reassigned to Anita");
        assertEquals(0, response.ordersDelayedCount(), "Order should not be delayed");

        ScheduleResult beforeRes = response.beforeSchedule().get(0);
        ScheduleResult afterRes = response.afterSchedule().get(0);

        // Verify machine and time are identical
        assertEquals("GRIND-01", afterRes.getMachine().getMachineCode());
        assertEquals(beforeRes.getStartTime(), afterRes.getStartTime());
        assertEquals(beforeRes.getEndTime(), afterRes.getEndTime());

        // Verify operator changed from Kumar to Anita
        assertEquals("Kumar", beforeRes.getOperator().getName());
        assertEquals("Anita", afterRes.getOperator().getName());

        // Verify Delta details
        OperationScheduleDelta delta = response.impactDeltas().get(0);
        assertEquals("Kumar", delta.beforeOperatorName());
        assertEquals("Anita", delta.afterOperatorName());
        assertTrue(delta.operatorChanged());
        assertFalse(delta.machineChanged());
        assertFalse(delta.timeChanged());
        assertEquals(0L, delta.delayMinutes());
    }

    @Test
    void replanSchedule_operatorAbsent_noReplacementAvailable_shiftsToFeasibleTime() {
        LocalDate today = LocalDate.now();
        LocalDateTime baseTime = LocalDateTime.of(today, LocalTime.of(6, 0));

        Order order1 = new Order("ORD-001", null, 10, "SHAFT", baseTime.plusDays(3), "OPEN");
        ReflectionTestUtils.setField(order1, "id", 101L);
        Operation op1 = new Operation(order1, 1, "GRINDING", 60, "GRINDING");
        ReflectionTestUtils.setField(op1, "id", 1001L);

        when(orderRepository.findByStatusIgnoreCase("OPEN")).thenReturn(List.of(order1));
        when(orderRepository.existsById(101L)).thenReturn(true);
        when(operationRepository.findAll()).thenReturn(List.of(op1));

        when(machineCapabilityRepository.findByCapabilityIgnoreCase("GRINDING"))
                .thenReturn(List.of(new MachineCapability(grind1, "GRINDING")));

        // Only Kumar is qualified for GRINDING
        when(operatorSkillRepository.findBySkillNameIgnoreCase("GRINDING"))
                .thenReturn(List.of(new OperatorSkill(operatorKumar, "GRINDING")));

        // Kumar works Shift 1 (06:00-14:00) and Shift 2 (14:00-22:00)
        OperatorShift shiftKumarTodayS1 = new OperatorShift(operatorKumar, shift1, today, true);
        OperatorShift shiftKumarTodayS2 = new OperatorShift(operatorKumar, shift2, today, true);

        when(operatorShiftRepository.findByOperatorIdAndWorkDateAndAvailableTrue(any(), any()))
                .thenAnswer(inv -> {
                    Long opId = inv.getArgument(0);
                    LocalDate d = inv.getArgument(1);
                    if (today.equals(d) && Long.valueOf(201L).equals(opId)) {
                        return List.of(shiftKumarTodayS1, shiftKumarTodayS2);
                    }
                    return List.of();
                });

        when(breakdownRepository.findAll()).thenReturn(List.of());

        // Kumar is absent from 06:00 to 14:00
        LocalDateTime absenceStart = LocalDateTime.of(today, LocalTime.of(6, 0));
        LocalDateTime absenceEnd = LocalDateTime.of(today, LocalTime.of(14, 0));
        OperatorAbsence kumarAbsence = new OperatorAbsence(operatorKumar, absenceStart, absenceEnd, "Emergency Absence");

        when(operatorAbsenceRepository.findAll()).thenReturn(List.of(kumarAbsence));
        when(operatorAbsenceRepository.findByOperatorId(201L)).thenReturn(List.of(kumarAbsence));

        ReplanResultResponse response = schedulerService.replanSchedule(SchedulingStrategy.MOST_ON_TIME, baseTime, baseTime);

        assertNotNull(response);
        assertEquals(1, response.operationsMovedCount());

        ScheduleResult afterRes = response.afterSchedule().get(0);

        // Verify operation was moved to after absence (14:00 on Shift 2)
        assertEquals(LocalDateTime.of(today, LocalTime.of(14, 0)), afterRes.getStartTime());
        assertEquals(LocalDateTime.of(today, LocalTime.of(15, 0)), afterRes.getEndTime());
        assertEquals("GRIND-01", afterRes.getMachine().getMachineCode());
        assertEquals("Kumar", afterRes.getOperator().getName());
    }

    @Test
    void replanSchedule_combinedMachineBreakdownAndOperatorAbsence_defenseScenario() {
        LocalDate today = LocalDate.now();
        LocalDateTime baseTime = LocalDateTime.of(today, LocalTime.of(6, 0));

        Order order1 = new Order("ORD-001", null, 10, "SHAFT", baseTime.plusDays(3), "OPEN");
        ReflectionTestUtils.setField(order1, "id", 101L);
        Operation op1 = new Operation(order1, 1, "GRINDING", 60, "GRINDING");
        ReflectionTestUtils.setField(op1, "id", 1001L);

        when(orderRepository.findByStatusIgnoreCase("OPEN")).thenReturn(List.of(order1));
        when(orderRepository.existsById(101L)).thenReturn(true);
        when(operationRepository.findAll()).thenReturn(List.of(op1));

        when(machineCapabilityRepository.findByCapabilityIgnoreCase("GRINDING"))
                .thenReturn(List.of(new MachineCapability(grind1, "GRINDING")));

        when(operatorSkillRepository.findBySkillNameIgnoreCase("GRINDING"))
                .thenReturn(List.of(
                        new OperatorSkill(operatorKumar, "GRINDING"),
                        new OperatorSkill(operatorAnita, "GRINDING")));

        OperatorShift shiftKumarS1 = new OperatorShift(operatorKumar, shift1, today, true);
        OperatorShift shiftAnitaS2 = new OperatorShift(operatorAnita, shift2, today, true);

        when(operatorShiftRepository.findByOperatorIdAndWorkDateAndAvailableTrue(any(), any()))
                .thenAnswer(inv -> {
                    Long opId = inv.getArgument(0);
                    LocalDate d = inv.getArgument(1);
                    if (today.equals(d)) {
                        if (Long.valueOf(201L).equals(opId)) return List.of(shiftKumarS1);
                        if (Long.valueOf(202L).equals(opId)) return List.of(shiftAnitaS2);
                    }
                    return List.of();
                });

        // Defense Scenario:
        // 1. GRIND-01 is down from 06:00 to 14:00 (8 hours)
        LocalDateTime breakdownStart = LocalDateTime.of(today, LocalTime.of(6, 0));
        LocalDateTime breakdownEnd = LocalDateTime.of(today, LocalTime.of(14, 0));
        Breakdown grindBreakdown = new Breakdown(grind1, breakdownStart, breakdownEnd, "Spindle Overhaul");

        when(breakdownRepository.findAll()).thenReturn(List.of(grindBreakdown));
        when(breakdownRepository.findByMachineId(501L)).thenReturn(List.of(grindBreakdown));

        // 2. Kumar (Shift 1 grinder) is also absent from 06:00 to 14:00
        OperatorAbsence kumarAbsence = new OperatorAbsence(operatorKumar, breakdownStart, breakdownEnd, "Personal Leave");
        when(operatorAbsenceRepository.findAll()).thenReturn(List.of(kumarAbsence));
        when(operatorAbsenceRepository.findByOperatorId(201L)).thenReturn(List.of(kumarAbsence));
        when(operatorAbsenceRepository.findByOperatorId(202L)).thenReturn(List.of());

        ReplanResultResponse response = schedulerService.replanSchedule(SchedulingStrategy.MOST_ON_TIME, baseTime, baseTime);

        assertNotNull(response);

        ScheduleResult afterRes = response.afterSchedule().get(0);

        // After breakdown ends at 14:00, Shift 2 operator Anita takes over GRIND-01
        assertEquals(LocalDateTime.of(today, LocalTime.of(14, 0)), afterRes.getStartTime());
        assertEquals(LocalDateTime.of(today, LocalTime.of(15, 0)), afterRes.getEndTime());
        assertEquals("GRIND-01", afterRes.getMachine().getMachineCode());
        assertEquals("Anita", afterRes.getOperator().getName());
    }
}