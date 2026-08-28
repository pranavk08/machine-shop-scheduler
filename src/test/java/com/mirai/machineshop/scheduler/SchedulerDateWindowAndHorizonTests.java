package com.mirai.machineshop.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

import com.mirai.machineshop.entity.Changeover;
import com.mirai.machineshop.entity.Machine;
import com.mirai.machineshop.entity.MachineCapability;
import com.mirai.machineshop.entity.Operation;
import com.mirai.machineshop.entity.Operator;
import com.mirai.machineshop.entity.OperatorShift;
import com.mirai.machineshop.entity.OperatorSkill;
import com.mirai.machineshop.entity.Order;
import com.mirai.machineshop.entity.Shift;
import com.mirai.machineshop.exception.SchedulingUnavailableException;
import com.mirai.machineshop.repository.BreakdownRepository;
import com.mirai.machineshop.repository.ChangeoverRepository;
import com.mirai.machineshop.repository.MachineCapabilityRepository;
import com.mirai.machineshop.repository.OperationRepository;
import com.mirai.machineshop.repository.OperatorShiftRepository;
import com.mirai.machineshop.repository.OperatorSkillRepository;
import com.mirai.machineshop.repository.OrderRepository;

class SchedulerDateWindowAndHorizonTests {

    private MachineCapabilityRepository machineCapabilityRepository;
    private OperationRepository operationRepository;
    private OrderRepository orderRepository;
    private OperatorSkillRepository operatorSkillRepository;
    private OperatorShiftRepository operatorShiftRepository;
    private ChangeoverRepository changeoverRepository;
    private BreakdownRepository breakdownRepository;
    private SchedulerService schedulerService;

    private Machine machine;
    private Operator operator;
    private Shift dayShift;
    private Shift nightShift;

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

        machine = new Machine("CNC-01", "CNC Lathe", "TURNING");
        ReflectionTestUtils.setField(machine, "id", 1L);

        operator = new Operator("OP-001", "Ravi");
        ReflectionTestUtils.setField(operator, "id", 1L);

        dayShift = new Shift("SHIFT-1", LocalTime.of(6, 0), LocalTime.of(14, 0));
        ReflectionTestUtils.setField(dayShift, "id", 1L);

        nightShift = new Shift("SHIFT-NIGHT", LocalTime.of(22, 0), LocalTime.of(6, 0));
        ReflectionTestUtils.setField(nightShift, "id", 2L);

        when(machineCapabilityRepository.findByCapabilityIgnoreCase("TURNING"))
                .thenReturn(List.of(new MachineCapability(machine, "TURNING")));
        when(operatorSkillRepository.findBySkillNameIgnoreCase("TURNING"))
                .thenReturn(List.of(new OperatorSkill(operator, "TURNING")));
        when(breakdownRepository.findByMachineId(any())).thenReturn(List.of());
        when(changeoverRepository.findByMachineIdAndFromPartFamilyIgnoreCaseAndToPartFamilyIgnoreCase(
                any(), any(), any())).thenReturn(Optional.empty());
    }

    @Test
    void dynamicOperatorShiftDates_allowsSchedulingInFutureDate() {
        LocalDate futureDate = LocalDate.now().plusMonths(3);
        LocalDateTime startTime = LocalDateTime.of(futureDate, LocalTime.of(8, 0));

        Order order = new Order("ORD-FUTURE", null, 100, "SHAFT", startTime.plusDays(2), "OPEN");
        ReflectionTestUtils.setField(order, "id", 100L);
        Operation operation = new Operation(order, 1, "TURNING", 60, "TURNING");
        ReflectionTestUtils.setField(operation, "id", 1001L);

        when(orderRepository.existsById(100L)).thenReturn(true);
        when(operationRepository.findAll()).thenReturn(List.of(operation));

        OperatorShift shift = new OperatorShift(operator, dayShift, futureDate, true);
        when(operatorShiftRepository.findByOperatorIdAndWorkDateAndAvailableTrue(operator.getId(), futureDate))
                .thenReturn(List.of(shift));

        List<ScheduleResult> results = schedulerService.scheduleOrder(100L, startTime);

        assertEquals(1, results.size());
        assertEquals(startTime, results.get(0).getStartTime());
        assertEquals(startTime.plusMinutes(60), results.get(0).getEndTime());
        assertEquals(operator, results.get(0).getOperator());
        assertEquals(machine, results.get(0).getMachine());
    }

    @Test
    void schedulingOutsideOldHardcodedDateRange_succeeds() {
        LocalDate outsideOldRangeDate = LocalDate.of(2027, 5, 10);
        LocalDateTime startTime = LocalDateTime.of(outsideOldRangeDate, LocalTime.of(9, 0));

        Order order = new Order("ORD-2027", null, 50, "SHAFT", startTime.plusDays(1), "OPEN");
        ReflectionTestUtils.setField(order, "id", 200L);
        Operation operation = new Operation(order, 1, "TURNING", 90, "TURNING");
        ReflectionTestUtils.setField(operation, "id", 2001L);

        when(orderRepository.existsById(200L)).thenReturn(true);
        when(operationRepository.findAll()).thenReturn(List.of(operation));

        OperatorShift shift = new OperatorShift(operator, dayShift, outsideOldRangeDate, true);
        when(operatorShiftRepository.findByOperatorIdAndWorkDateAndAvailableTrue(operator.getId(), outsideOldRangeDate))
                .thenReturn(List.of(shift));

        List<ScheduleResult> results = schedulerService.scheduleOrder(200L, startTime);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(startTime, results.get(0).getStartTime());
        assertEquals(startTime.plusMinutes(90), results.get(0).getEndTime());
    }

    @Test
    void overnightShift_coversOperationCrossingMidnight() {
        LocalDate shiftStartDate = LocalDate.of(2026, 9, 1);
        OperatorShift overnightOpShift = new OperatorShift(operator, nightShift, shiftStartDate, true);

        // Operation starts at 23:00 on day 1 and finishes at 01:30 on day 2
        LocalDateTime opStart = LocalDateTime.of(shiftStartDate, LocalTime.of(23, 0));
        LocalDateTime opEnd = LocalDateTime.of(shiftStartDate.plusDays(1), LocalTime.of(1, 30));

        when(operatorShiftRepository.findByOperatorIdAndWorkDateAndAvailableTrue(operator.getId(), shiftStartDate))
                .thenReturn(List.of(overnightOpShift));

        List<Operator> available = schedulerService.findAvailableOperators("TURNING", shiftStartDate, opStart, opEnd);

        assertFalse(available.isEmpty());
        assertEquals(1, available.size());
        assertEquals(operator, available.get(0));
    }

    @Test
    void overnightShift_coversOperationInEarlyMorningOfNextDay() {
        LocalDate shiftStartDate = LocalDate.of(2026, 9, 1);
        LocalDate nextDay = shiftStartDate.plusDays(1);
        OperatorShift overnightOpShift = new OperatorShift(operator, nightShift, shiftStartDate, true);

        // Operation starts at 02:00 and finishes at 04:00 on day 2 (covered by night shift started on day 1)
        LocalDateTime opStart = LocalDateTime.of(nextDay, LocalTime.of(2, 0));
        LocalDateTime opEnd = LocalDateTime.of(nextDay, LocalTime.of(4, 0));

        when(operatorShiftRepository.findByOperatorIdAndWorkDateAndAvailableTrue(operator.getId(), nextDay))
                .thenReturn(List.of());
        when(operatorShiftRepository.findByOperatorIdAndWorkDateAndAvailableTrue(operator.getId(), shiftStartDate))
                .thenReturn(List.of(overnightOpShift));

        List<Operator> available = schedulerService.findAvailableOperators("TURNING", nextDay, opStart, opEnd);

        assertFalse(available.isEmpty());
        assertEquals(1, available.size());
        assertEquals(operator, available.get(0));
    }

    @Test
    void overnightShift_rejectsOperationAfterShiftEnd() {
        LocalDate shiftStartDate = LocalDate.of(2026, 9, 1);
        LocalDate nextDay = shiftStartDate.plusDays(1);
        OperatorShift overnightOpShift = new OperatorShift(operator, nightShift, shiftStartDate, true);

        // Operation starts at 06:30 on day 2 (night shift ended at 06:00)
        LocalDateTime opStart = LocalDateTime.of(nextDay, LocalTime.of(6, 30));
        LocalDateTime opEnd = LocalDateTime.of(nextDay, LocalTime.of(8, 0));

        when(operatorShiftRepository.findByOperatorIdAndWorkDateAndAvailableTrue(operator.getId(), nextDay))
                .thenReturn(List.of());
        when(operatorShiftRepository.findByOperatorIdAndWorkDateAndAvailableTrue(operator.getId(), shiftStartDate))
                .thenReturn(List.of(overnightOpShift));

        List<Operator> available = schedulerService.findAvailableOperators("TURNING", nextDay, opStart, opEnd);

        assertTrue(available.isEmpty());
    }

    @Test
    void schedulerSearchHorizon_findsSlotsBeyond48Hours() {
        LocalDate baseDate = LocalDate.of(2026, 9, 1);
        LocalDateTime baseTime = LocalDateTime.of(baseDate, LocalTime.of(6, 0));

        Order order1 = new Order("ORD-1", null, 10, "SHAFT", baseTime.plusDays(10), "OPEN");
        ReflectionTestUtils.setField(order1, "id", 1L);
        // First operation takes 60 hours (2.5 days), pushing downstream scheduling past 48 hours
        Operation op1 = new Operation(order1, 1, "TURNING", 180, "TURNING");
        ReflectionTestUtils.setField(op1, "id", 11L);

        Order order2 = new Order("ORD-2", null, 10, "SHAFT", baseTime.plusDays(10), "OPEN");
        ReflectionTestUtils.setField(order2, "id", 2L);
        Operation op2 = new Operation(order2, 1, "TURNING", 60, "TURNING");
        ReflectionTestUtils.setField(op2, "id", 21L);

        when(orderRepository.existsById(2L)).thenReturn(true);
        when(operationRepository.findAll()).thenReturn(List.of(op2));

        // Shift is available on day 4 (72 hours later)
        LocalDate day4 = baseDate.plusDays(3);
        OperatorShift day4Shift = new OperatorShift(operator, dayShift, day4, true);
        when(operatorShiftRepository.findByOperatorIdAndWorkDateAndAvailableTrue(eq(operator.getId()), any()))
                .thenAnswer(invocation -> {
                    LocalDate queriedDate = invocation.getArgument(1);
                    if (queriedDate.equals(day4)) {
                        return List.of(day4Shift);
                    }
                    return List.of();
                });

        // Request scheduling starting at baseTime, but only day 4 is available for operator
        List<ScheduleResult> results = schedulerService.scheduleOrder(2L, baseTime);

        assertNotNull(results);
        assertEquals(1, results.size());
        // Start time should be on day 4 at 06:00
        assertEquals(LocalDateTime.of(day4, LocalTime.of(6, 0)), results.get(0).getStartTime());
    }
}
