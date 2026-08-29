package com.mirai.machineshop.scheduler;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.mirai.machineshop.dto.ReplanResultResponse;
import com.mirai.machineshop.dto.StrategyComparisonResponse;
import com.mirai.machineshop.entity.Breakdown;
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

class SchedulingPerformanceTests {

    private MachineCapabilityRepository machineCapabilityRepository;
    private OperationRepository operationRepository;
    private OrderRepository orderRepository;
    private OperatorSkillRepository operatorSkillRepository;
    private OperatorShiftRepository operatorShiftRepository;
    private ChangeoverRepository changeoverRepository;
    private BreakdownRepository breakdownRepository;
    private CostCalculationService costCalculationService;
    private SchedulerService schedulerService;

    private List<Order> mockOrders;
    private List<Operation> mockOperations;

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
                changeoverRepository, 480, 500.0, 150.0, 75.0, 300.0);

        schedulerService = new SchedulerService(
                machineCapabilityRepository,
                operationRepository,
                orderRepository,
                operatorSkillRepository,
                operatorShiftRepository,
                changeoverRepository,
                breakdownRepository,
                costCalculationService);

        // 14 Machines
        List<Machine> machines = new ArrayList<>();
        List<MachineCapability> capabilities = new ArrayList<>();
        String[][] machineData = {
                {"CNC-01", "TURNING"}, {"CNC-02", "TURNING"}, {"CNC-03", "TURNING"}, {"CNC-04", "TURNING"},
                {"CNC-05", "TURNING"}, {"CNC-06", "TURNING"},
                {"MILL-01", "MILLING"}, {"MILL-02", "MILLING"}, {"MILL-03", "MILLING"}, {"MILL-04", "MILLING"},
                {"DRILL-01", "DRILLING"}, {"DRILL-02", "DRILLING"}, {"DRILL-03", "DRILLING"},
                {"GRIND-01", "GRINDING"}
        };
        long mId = 1;
        for (String[] m : machineData) {
            Machine machine = new Machine(m[0], m[0], m[1]);
            ReflectionTestUtils.setField(machine, "id", mId++);
            machines.add(machine);
            capabilities.add(new MachineCapability(machine, m[1]));
        }
        when(machineCapabilityRepository.findAll()).thenReturn(capabilities);

        // 10 Operators
        List<Operator> operators = new ArrayList<>();
        List<OperatorSkill> skills = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            Operator op = new Operator(String.format("OP-%03d", i), "Operator " + i);
            ReflectionTestUtils.setField(op, "id", (long) i);
            operators.add(op);
            skills.add(new OperatorSkill(op, "TURNING"));
            skills.add(new OperatorSkill(op, "MILLING"));
            skills.add(new OperatorSkill(op, "DRILLING"));
            skills.add(new OperatorSkill(op, "GRINDING"));
        }
        when(operatorSkillRepository.findAll()).thenReturn(skills);

        // Shifts for 30 days
        Shift shift1 = new Shift("SHIFT-1", LocalTime.of(6, 0), LocalTime.of(14, 0));
        ReflectionTestUtils.setField(shift1, "id", 1L);
        Shift shift2 = new Shift("SHIFT-2", LocalTime.of(14, 0), LocalTime.of(22, 0));
        ReflectionTestUtils.setField(shift2, "id", 2L);

        List<OperatorShift> shifts = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int day = 0; day < 30; day++) {
            LocalDate date = today.plusDays(day);
            for (Operator op : operators) {
                shifts.add(new OperatorShift(op, shift1, date, true));
                shifts.add(new OperatorShift(op, shift2, date, true));
            }
        }
        when(operatorShiftRepository.findAll()).thenReturn(shifts);

        // 25 Orders with 75 Operations
        mockOrders = new ArrayList<>();
        mockOperations = new ArrayList<>();
        Customer custTier1 = new Customer("CUST-001", "Apex Auto", "TIER-1");
        Customer custTier2 = new Customer("CUST-002", "Delta Auto", "TIER-2");

        long opId = 1000;
        for (int i = 1; i <= 25; i++) {
            String num = String.format("ORD-%03d", i);
            Customer cust = (i % 3 == 0) ? custTier1 : custTier2;
            String partFam = (i % 3 == 0) ? "SHAFT" : ((i % 3 == 1) ? "GEAR" : "HOUSING");
            Order order = new Order(num, cust, 1000, partFam, LocalDateTime.now().plusDays(2 + i), "OPEN");
            ReflectionTestUtils.setField(order, "id", (long) i);
            mockOrders.add(order);

            Operation op1 = new Operation(order, 1, "TURNING", 90, "TURNING");
            ReflectionTestUtils.setField(op1, "id", opId++);
            Operation op2 = new Operation(order, 2, "MILLING", 60, "MILLING");
            ReflectionTestUtils.setField(op2, "id", opId++);
            Operation op3 = new Operation(order, 3, "GRINDING", 45, "GRINDING");
            ReflectionTestUtils.setField(op3, "id", opId++);

            mockOperations.add(op1);
            mockOperations.add(op2);
            mockOperations.add(op3);
        }

        when(orderRepository.findByStatusIgnoreCase("OPEN")).thenReturn(mockOrders);
        when(operationRepository.findAll()).thenReturn(mockOperations);
        when(breakdownRepository.findAll()).thenReturn(List.of());
        when(changeoverRepository.findAll()).thenReturn(List.of());
    }

    @Test
    void scheduleAllOpenOrders_executesSubSecond() {
        // Warmup
        schedulerService.scheduleAllOpenOrders();

        long start = System.currentTimeMillis();
        List<ScheduleResult> schedule = schedulerService.scheduleAllOpenOrders();
        long elapsed = System.currentTimeMillis() - start;

        assertNotNull(schedule);
        assertTrue(schedule.size() >= 75, "Expected all operations to be scheduled");
        assertTrue(elapsed < 1000, "Schedule all open orders took " + elapsed + "ms, expected < 1000ms");
        System.out.println("⚡ Performance: scheduleAllOpenOrders took " + elapsed + " ms for " + schedule.size() + " operations.");
    }

    @Test
    void compareStrategies_executesFast() {
        long start = System.currentTimeMillis();
        StrategyComparisonResponse response = schedulerService.compareStrategies();
        long elapsed = System.currentTimeMillis() - start;

        assertNotNull(response);
        assertTrue(response.strategies().size() == 3);
        assertTrue(elapsed < 2000, "compareStrategies took " + elapsed + "ms, expected < 2000ms");
        System.out.println("⚡ Performance: compareStrategies (3 full schedules) took " + elapsed + " ms.");
    }

    @Test
    void replanSchedule_executesSubSecond() {
        Breakdown breakdown = new Breakdown(
                new Machine("CNC-01", "CNC Lathe 1", "TURNING"),
                LocalDateTime.now().plusHours(2),
                LocalDateTime.now().plusHours(6),
                "Tooling replacement"
        );
        when(breakdownRepository.findAll()).thenReturn(List.of(breakdown));

        long start = System.currentTimeMillis();
        ReplanResultResponse response = schedulerService.replanSchedule(LocalDateTime.now());
        long elapsed = System.currentTimeMillis() - start;

        assertNotNull(response);
        assertTrue(elapsed < 1000, "replanSchedule took " + elapsed + "ms, expected < 1000ms");
        System.out.println("⚡ Performance: replanSchedule took " + elapsed + " ms.");
    }
}
