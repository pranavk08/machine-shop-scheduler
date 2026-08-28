package com.mirai.machineshop.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

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

class SchedulerServiceOperatorAvailabilityTests {

    private static final LocalDateTime START_TIME =
            LocalDateTime.of(2026, 8, 28, 6, 0);

    @Test
    void availableQualifiedOperatorCanBeAssignedToAnOrderOperation() {
        SchedulerTestContext context = new SchedulerTestContext();
        Operator operator = context.operator(true);

        when(context.operatorSkillRepository
                .findBySkillNameIgnoreCase("TURNING"))
                .thenReturn(List.of(new OperatorSkill(operator, "TURNING")));
        when(context.operatorShiftRepository
                .findByOperatorIdAndWorkDateAndAvailableTrue(
                        operator.getId(), START_TIME.toLocalDate()))
                .thenReturn(List.of(context.operatorShift(operator)));

        List<ScheduleResult> schedule =
                context.service.scheduleOrder(1L, START_TIME);

        assertEquals(1, schedule.size());
        assertEquals(operator, schedule.get(0).getOperator());
    }

    @Test
    void unavailableQualifiedOperatorIsExcludedFromAvailability() {
        SchedulerTestContext context = new SchedulerTestContext();
        Operator operator = context.operator(false);

        when(context.operatorSkillRepository
                .findBySkillNameIgnoreCase("TURNING"))
                .thenReturn(List.of(new OperatorSkill(operator, "TURNING")));

        List<Operator> availableOperators =
                context.service.findAvailableOperators(
                        "TURNING",
                        START_TIME.toLocalDate(),
                        START_TIME,
                        START_TIME.plusMinutes(30));

        assertEquals(List.of(), availableOperators);
    }

    @Test
    void schedulerRejectsOrderWhenAllQualifiedOperatorsAreUnavailable() {
        SchedulerTestContext context = new SchedulerTestContext();
        Operator firstOperator = context.operator(false);
        Operator secondOperator = context.operator(false);
        ReflectionTestUtils.setField(secondOperator, "id", 2L);

        when(context.operatorSkillRepository
                .findBySkillNameIgnoreCase("TURNING"))
                .thenReturn(List.of(
                        new OperatorSkill(firstOperator, "TURNING"),
                        new OperatorSkill(secondOperator, "TURNING")));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> context.service.scheduleOrder(1L, START_TIME));

        assertEquals(
                "No machine and operator available for operation: TURNING",
                exception.getMessage());
    }

    private static class SchedulerTestContext {

        private final MachineCapabilityRepository machineCapabilityRepository =
                mock(MachineCapabilityRepository.class);
        private final OperationRepository operationRepository =
                mock(OperationRepository.class);
        private final OrderRepository orderRepository = mock(OrderRepository.class);
        private final OperatorSkillRepository operatorSkillRepository =
                mock(OperatorSkillRepository.class);
        private final OperatorShiftRepository operatorShiftRepository =
                mock(OperatorShiftRepository.class);
        private final ChangeoverRepository changeoverRepository =
                mock(ChangeoverRepository.class);
        private final BreakdownRepository breakdownRepository =
                mock(BreakdownRepository.class);
        private final SchedulerService service = new SchedulerService(
                machineCapabilityRepository,
                operationRepository,
                orderRepository,
                operatorSkillRepository,
                operatorShiftRepository,
                changeoverRepository,
                breakdownRepository);

        private SchedulerTestContext() {
            Machine machine = new Machine("M-001", "Turning", "TURNING");
            ReflectionTestUtils.setField(machine, "id", 1L);

            Order order = new Order(
                    "ORD-001", null, 1, "SHAFT", START_TIME.plusDays(1), "OPEN");
            ReflectionTestUtils.setField(order, "id", 1L);

            Operation operation = new Operation(order, 1, "TURNING", 30, "TURNING");
            ReflectionTestUtils.setField(operation, "id", 1L);

            when(machineCapabilityRepository.findByCapabilityIgnoreCase("TURNING"))
                    .thenReturn(List.of(new MachineCapability(machine, "TURNING")));
            when(operationRepository.findAll()).thenReturn(List.of(operation));
            when(breakdownRepository.findByMachineId(machine.getId()))
                    .thenReturn(List.of());
        }

        private Operator operator(boolean available) {
            Operator operator = new Operator("OP-001", "Operator");
            operator.setAvailable(available);
            ReflectionTestUtils.setField(operator, "id", 1L);
            return operator;
        }

        private OperatorShift operatorShift(Operator operator) {
            Shift shift = new Shift(
                    "DAY", LocalTime.of(6, 0), LocalTime.of(14, 0));
            return new OperatorShift(operator, shift, LocalDate.of(2026, 8, 28), true);
        }
    }
}
