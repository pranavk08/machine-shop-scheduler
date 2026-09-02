package com.mirai.machineshop.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import com.mirai.machineshop.entity.Customer;
import com.mirai.machineshop.entity.Machine;
import com.mirai.machineshop.entity.MachineCapability;
import com.mirai.machineshop.entity.MaterialDelay;
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
public class MaterialDelayReplanningTests {

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

    private SchedulerService schedulerService;

    private Machine cnc1;
    private Machine mill1;
    private Operator operatorRavi;
    private Operator operatorMahesh;
    private Shift shift1;

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
                materialDelayRepository,
                costCalculationService);

        cnc1 = new Machine("CNC-01", "CNC Lathe 1", "TURNING");
        ReflectionTestUtils.setField(cnc1, "id", 101L);

        mill1 = new Machine("MILL-01", "Milling 1", "MILLING");
        ReflectionTestUtils.setField(mill1, "id", 102L);

        operatorRavi = new Operator("OP-001", "Ravi");
        ReflectionTestUtils.setField(operatorRavi, "id", 201L);

        operatorMahesh = new Operator("OP-004", "Mahesh");
        ReflectionTestUtils.setField(operatorMahesh, "id", 204L);

        shift1 = new Shift("Shift 1", LocalTime.of(6, 0), LocalTime.of(14, 0));
        ReflectionTestUtils.setField(shift1, "id", 1L);
    }

    @Test
    void replanSchedule_materialDelay_shiftsOperationsPostArrival() {
        LocalDate today = LocalDate.now();
        LocalDateTime baseTime = LocalDateTime.of(today, LocalTime.of(6, 0));

        Customer cust = new Customer("CUST-1", "Acme Corp", "Tier 1");
        Order order1 = new Order("ORD-001", cust, 10, "SHAFT", baseTime.plusDays(3), "OPEN");
        ReflectionTestUtils.setField(order1, "id", 1L);

        Operation op1 = new Operation(order1, 1, "TURNING", 60, "TURNING");
        ReflectionTestUtils.setField(op1, "id", 11L);
        Operation op2 = new Operation(order1, 2, "MILLING", 60, "MILLING");
        ReflectionTestUtils.setField(op2, "id", 12L);

        MachineCapability capCnc = new MachineCapability(cnc1, "TURNING");
        MachineCapability capMill = new MachineCapability(mill1, "MILLING");
        when(machineCapabilityRepository.findAll()).thenReturn(List.of(capCnc, capMill));

        OperatorSkill skillTurning = new OperatorSkill(operatorRavi, "TURNING");
        OperatorSkill skillMilling = new OperatorSkill(operatorMahesh, "MILLING");
        when(operatorSkillRepository.findAll()).thenReturn(List.of(skillTurning, skillMilling));

        OperatorShift shiftRavi = new OperatorShift(operatorRavi, shift1, today, true);
        OperatorShift shiftMahesh = new OperatorShift(operatorMahesh, shift1, today, true);
        when(operatorShiftRepository.findAll()).thenReturn(List.of(shiftRavi, shiftMahesh));

        when(orderRepository.findByStatusIgnoreCase("OPEN")).thenReturn(List.of(order1));
        when(operationRepository.findAll()).thenReturn(List.of(op1, op2));

        // Baseline has NO disruptions
        when(breakdownRepository.findAll()).thenReturn(List.of());
        when(operatorAbsenceRepository.findAll()).thenReturn(List.of());
        when(materialDelayRepository.findAll()).thenReturn(List.of());

        // Now introduce Material Delay for ORD-001: delayed until 10:00 AM
        LocalDateTime materialArrival = LocalDateTime.of(today, LocalTime.of(10, 0));
        MaterialDelay delay = new MaterialDelay(order1, materialArrival, "Supplier steel shipment delayed");
        ReflectionTestUtils.setField(delay, "id", 301L);

        when(materialDelayRepository.findAll()).thenReturn(List.of(delay));
        when(materialDelayRepository.findByOrderId(1L)).thenReturn(List.of(delay));

        ReplanResultResponse result = schedulerService.replanSchedule();

        assertNotNull(result);
        assertEquals(2, result.totalOperations());
        assertEquals(2, result.operationsMovedCount()); // Both Op 1 and Op 2 shifted

        // Verify After Schedule: Op 1 must start at or after materialArrival (10:00 AM)
        ScheduleResult afterOp1 = result.afterSchedule().stream()
                .filter(r -> r.getOperation().getId().equals(11L))
                .findFirst().orElseThrow();
        ScheduleResult afterOp2 = result.afterSchedule().stream()
                .filter(r -> r.getOperation().getId().equals(12L))
                .findFirst().orElseThrow();

        assertFalse(afterOp1.getStartTime().isBefore(materialArrival),
                "Op 1 start time (" + afterOp1.getStartTime() + ") must be >= materialArrival (" + materialArrival + ")");
        assertFalse(afterOp2.getStartTime().isBefore(afterOp1.getEndTime()),
                "Op 2 start time (" + afterOp2.getStartTime() + ") must be >= Op 1 end time (" + afterOp1.getEndTime() + ")");

        // Preserved machine assignments
        assertEquals("CNC-01", afterOp1.getMachine().getMachineCode());
        assertEquals("MILL-01", afterOp2.getMachine().getMachineCode());
    }

    @Test
    void replanSchedule_materialDelay_unaffectedOrdersRemainUnchanged() {
        LocalDate today = LocalDate.now();
        LocalDateTime baseTime = LocalDateTime.of(today, LocalTime.of(6, 0));

        Customer cust = new Customer("CUST-1", "Acme Corp", "Tier 1");
        Order order1 = new Order("ORD-001", cust, 10, "SHAFT", baseTime.plusDays(3), "OPEN");
        ReflectionTestUtils.setField(order1, "id", 1L);

        Order order2 = new Order("ORD-002", cust, 10, "SHAFT", baseTime.plusDays(3), "OPEN");
        ReflectionTestUtils.setField(order2, "id", 2L);

        Operation op1_1 = new Operation(order1, 1, "TURNING", 60, "TURNING");
        ReflectionTestUtils.setField(op1_1, "id", 11L);

        Operation op2_1 = new Operation(order2, 1, "MILLING", 60, "MILLING");
        ReflectionTestUtils.setField(op2_1, "id", 21L);

        MachineCapability capCnc = new MachineCapability(cnc1, "TURNING");
        MachineCapability capMill = new MachineCapability(mill1, "MILLING");
        when(machineCapabilityRepository.findAll()).thenReturn(List.of(capCnc, capMill));

        OperatorSkill skillTurning = new OperatorSkill(operatorRavi, "TURNING");
        OperatorSkill skillMilling = new OperatorSkill(operatorMahesh, "MILLING");
        when(operatorSkillRepository.findAll()).thenReturn(List.of(skillTurning, skillMilling));

        OperatorShift shiftRavi = new OperatorShift(operatorRavi, shift1, today, true);
        OperatorShift shiftMahesh = new OperatorShift(operatorMahesh, shift1, today, true);
        when(operatorShiftRepository.findAll()).thenReturn(List.of(shiftRavi, shiftMahesh));

        when(orderRepository.findByStatusIgnoreCase("OPEN")).thenReturn(List.of(order1, order2));
        when(operationRepository.findAll()).thenReturn(List.of(op1_1, op2_1));

        // Baseline: no disruptions
        when(breakdownRepository.findAll()).thenReturn(List.of());
        when(operatorAbsenceRepository.findAll()).thenReturn(List.of());
        when(materialDelayRepository.findAll()).thenReturn(List.of());

        // Material Delay ONLY on ORD-001
        LocalDateTime materialArrival = LocalDateTime.of(today, LocalTime.of(10, 0));
        MaterialDelay delay = new MaterialDelay(order1, materialArrival, "Supplier delay");
        ReflectionTestUtils.setField(delay, "id", 301L);

        when(materialDelayRepository.findAll()).thenReturn(List.of(delay));
        when(materialDelayRepository.findByOrderId(1L)).thenReturn(List.of(delay));
        when(materialDelayRepository.findByOrderId(2L)).thenReturn(List.of());

        ReplanResultResponse result = schedulerService.replanSchedule();

        assertNotNull(result);
        assertEquals(2, result.totalOperations());
        assertEquals(1, result.operationsMovedCount()); // Only ORD-001 moved
        assertEquals(1, result.impactDeltas().size()); // Only ORD-001 is in deltas

        // ORD-002 op2_1 must remain completely untouched in before vs after
        ScheduleResult beforeOp2 = result.beforeSchedule().stream()
                .filter(r -> "ORD-002".equals(r.getOperation().getOrder().getOrderNumber()))
                .findFirst().orElseThrow();
        ScheduleResult afterOp2 = result.afterSchedule().stream()
                .filter(r -> "ORD-002".equals(r.getOperation().getOrder().getOrderNumber()))
                .findFirst().orElseThrow();

        assertEquals(beforeOp2.getMachine().getId(), afterOp2.getMachine().getId());
        assertEquals(beforeOp2.getOperator().getId(), afterOp2.getOperator().getId());
        assertEquals(beforeOp2.getStartTime(), afterOp2.getStartTime());
        assertEquals(beforeOp2.getEndTime(), afterOp2.getEndTime());
    }

    @Test
    void replanSchedule_compoundDisruptions_breakdownAndAbsenceAndMaterialDelay_resolvesCleanly() {
        LocalDate today = LocalDate.now();
        LocalDateTime baseTime = LocalDateTime.of(today, LocalTime.of(6, 0));

        Customer cust = new Customer("CUST-1", "Acme Corp", "Tier 1");
        Order order1 = new Order("ORD-001", cust, 10, "SHAFT", baseTime.plusDays(3), "OPEN");
        ReflectionTestUtils.setField(order1, "id", 1L);

        Operation op1 = new Operation(order1, 1, "TURNING", 60, "TURNING");
        ReflectionTestUtils.setField(op1, "id", 11L);

        MachineCapability capCnc = new MachineCapability(cnc1, "TURNING");
        when(machineCapabilityRepository.findAll()).thenReturn(List.of(capCnc));

        OperatorSkill skillTurning = new OperatorSkill(operatorRavi, "TURNING");
        when(operatorSkillRepository.findAll()).thenReturn(List.of(skillTurning));

        OperatorShift shiftRavi = new OperatorShift(operatorRavi, shift1, today, true);
        when(operatorShiftRepository.findAll()).thenReturn(List.of(shiftRavi));

        when(orderRepository.findByStatusIgnoreCase("OPEN")).thenReturn(List.of(order1));
        when(operationRepository.findAll()).thenReturn(List.of(op1));

        // Compound disruptions:
        // 1. Machine breakdown: CNC-01 down from 06:00 to 07:00
        Breakdown breakdown = new Breakdown(cnc1, baseTime, baseTime.plusHours(1), "Maintenance");
        ReflectionTestUtils.setField(breakdown, "id", 101L);

        // 2. Operator absence: Ravi absent from 07:00 to 08:00
        OperatorAbsence absence = new OperatorAbsence(operatorRavi, baseTime.plusHours(1), baseTime.plusHours(2), "Doctor visit");
        ReflectionTestUtils.setField(absence, "id", 201L);

        // 3. Material delay: ORD-001 delayed until 08:30 AM
        MaterialDelay delay = new MaterialDelay(order1, baseTime.plusMinutes(150), "Customs hold");
        ReflectionTestUtils.setField(delay, "id", 301L);

        when(breakdownRepository.findAll()).thenReturn(List.of(breakdown));
        when(operatorAbsenceRepository.findAll()).thenReturn(List.of(absence));
        when(materialDelayRepository.findAll()).thenReturn(List.of(delay));
        when(materialDelayRepository.findByOrderId(1L)).thenReturn(List.of(delay));

        ReplanResultResponse result = schedulerService.replanSchedule();

        assertNotNull(result);
        assertEquals(1, result.totalOperations());
        assertEquals(1, result.operationsMovedCount());

        ScheduleResult after = result.afterSchedule().get(0);
        // Must start at or after 08:30 (when material is available, machine is up, and operator is back)
        assertFalse(after.getStartTime().isBefore(baseTime.plusMinutes(150)));
        assertEquals("CNC-01", after.getMachine().getMachineCode());
        assertEquals("OP-001", after.getOperator().getOperatorCode());
    }
}
