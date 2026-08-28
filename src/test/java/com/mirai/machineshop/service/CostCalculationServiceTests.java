package com.mirai.machineshop.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.mirai.machineshop.dto.CostImpactSummary;
import com.mirai.machineshop.dto.LateOrderSummary;
import com.mirai.machineshop.dto.OperatorOvertimeSummary;
import com.mirai.machineshop.entity.Customer;
import com.mirai.machineshop.entity.Machine;
import com.mirai.machineshop.entity.Operation;
import com.mirai.machineshop.entity.Operator;
import com.mirai.machineshop.entity.Order;
import com.mirai.machineshop.repository.ChangeoverRepository;
import com.mirai.machineshop.scheduler.ScheduleResult;

class CostCalculationServiceTests {

    private ChangeoverRepository changeoverRepository;
    private CostCalculationService costCalculationService;

    private Customer tier1Customer;
    private Customer tier2Customer;
    private Operator operatorRavi;
    private Operator operatorKumar;
    private Machine lathe1;

    @BeforeEach
    void setUp() {
        changeoverRepository = mock(ChangeoverRepository.class);
        // Regular shift: 480 mins (8h), OT rate: ₹500/hr, Tier 1 penalty: ₹150/hr, Tier 2 penalty: ₹75/hr, Changeover: ₹300/hr
        costCalculationService = new CostCalculationService(
                changeoverRepository,
                480,
                500.0,
                150.0,
                75.0,
                300.0
        );

        tier1Customer = new Customer("CUST-001", "Apex Auto Systems", "TIER-1");
        ReflectionTestUtils.setField(tier1Customer, "id", 1L);

        tier2Customer = new Customer("CUST-003", "Delta Components", "TIER-2");
        ReflectionTestUtils.setField(tier2Customer, "id", 2L);

        operatorRavi = new Operator("OP-001", "Ravi");
        ReflectionTestUtils.setField(operatorRavi, "id", 1L);

        operatorKumar = new Operator("OP-002", "Kumar");
        ReflectionTestUtils.setField(operatorKumar, "id", 2L);

        lathe1 = new Machine("CNC-01", "CNC Lathe 1", "TURNING");
        ReflectionTestUtils.setField(lathe1, "id", 1L);
    }

    @Test
    void calculateCostSummary_zeroOvertime_whenScheduledUnderCapacity() {
        LocalDate today = LocalDate.now();
        LocalDateTime startTime = LocalDateTime.of(today, java.time.LocalTime.of(8, 0));
        LocalDateTime endTime = startTime.plusMinutes(400); // 400 mins < 480 mins

        Order order = new Order("ORD-001", tier1Customer, 10, "SHAFT", startTime.plusDays(2), "OPEN");
        Operation op = new Operation(order, 1, "TURNING", 400, "TURNING");

        ScheduleResult res = new ScheduleResult(op, lathe1, operatorRavi, startTime, endTime);

        CostImpactSummary summary = costCalculationService.calculateCostSummary(List.of(res), List.of(order));

        assertNotNull(summary);
        assertEquals(0.0, summary.totalOvertimeHours());
        assertEquals(0.0, summary.totalOvertimeCost());
        assertTrue(summary.operatorOvertimes().isEmpty());
    }

    @Test
    void calculateCostSummary_calculatesOvertimeAccurately_whenExceedingCapacity() {
        LocalDate today = LocalDate.now();
        LocalDateTime op1Start = LocalDateTime.of(today, java.time.LocalTime.of(8, 0));
        LocalDateTime op1End = op1Start.plusMinutes(300); // 300 mins

        LocalDateTime op2Start = op1End;
        LocalDateTime op2End = op2Start.plusMinutes(300); // 300 mins => Total = 600 mins (10 hrs => 2 hrs OT)

        Order order = new Order("ORD-001", tier1Customer, 10, "SHAFT", op1Start.plusDays(2), "OPEN");
        Operation op1 = new Operation(order, 1, "TURNING", 300, "TURNING");
        Operation op2 = new Operation(order, 2, "TURNING", 300, "TURNING");

        ScheduleResult res1 = new ScheduleResult(op1, lathe1, operatorRavi, op1Start, op1End);
        ScheduleResult res2 = new ScheduleResult(op2, lathe1, operatorRavi, op2Start, op2End);

        CostImpactSummary summary = costCalculationService.calculateCostSummary(List.of(res1, res2), List.of(order));

        assertNotNull(summary);
        assertEquals(2.0, summary.totalOvertimeHours()); // 120 mins = 2.0 hrs
        assertEquals(1000.0, summary.totalOvertimeCost()); // 2.0 * 500 = 1000.0
        assertEquals(1, summary.operatorOvertimes().size());

        OperatorOvertimeSummary ot = summary.operatorOvertimes().get(0);
        assertEquals("OP-001", ot.operatorCode());
        assertEquals(600, ot.scheduledMinutes());
        assertEquals(480, ot.regularMinutes());
        assertEquals(120, ot.overtimeMinutes());
        assertEquals(1000.0, ot.overtimeCost());
    }

    @Test
    void calculateCostSummary_onTimeOrder_hasZeroPenalty() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime dueDate = now.plusDays(2);
        LocalDateTime completionTime = now.plusDays(1); // Finished 1 day before due date

        Order order = new Order("ORD-ONTIME", tier1Customer, 10, "SHAFT", dueDate, "OPEN");
        Operation op = new Operation(order, 1, "TURNING", 60, "TURNING");

        ScheduleResult res = new ScheduleResult(op, lathe1, operatorRavi, now, completionTime);

        CostImpactSummary summary = costCalculationService.calculateCostSummary(List.of(res), List.of(order));

        assertNotNull(summary);
        assertEquals(0, summary.lateOrdersCount());
        assertEquals(0.0, summary.totalPenaltyCost());
        assertTrue(summary.lateOrders().isEmpty());
    }

    @Test
    void calculateCostSummary_appliesDifferentiatedTier1AndTier2PenaltyRates() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime dueDate = now.plusHours(4);
        LocalDateTime completionTime = now.plusHours(8); // 4 hours late

        Order tier1Order = new Order("ORD-TIER1", tier1Customer, 10, "SHAFT", dueDate, "OPEN");
        ReflectionTestUtils.setField(tier1Order, "id", 101L);
        Operation op1 = new Operation(tier1Order, 1, "TURNING", 60, "TURNING");
        ScheduleResult res1 = new ScheduleResult(op1, lathe1, operatorRavi, now.plusHours(7), completionTime);

        Order tier2Order = new Order("ORD-TIER2", tier2Customer, 10, "SHAFT", dueDate, "OPEN");
        ReflectionTestUtils.setField(tier2Order, "id", 102L);
        Operation op2 = new Operation(tier2Order, 1, "TURNING", 60, "TURNING");
        ScheduleResult res2 = new ScheduleResult(op2, lathe1, operatorKumar, now.plusHours(7), completionTime);

        CostImpactSummary summary = costCalculationService.calculateCostSummary(
                List.of(res1, res2),
                List.of(tier1Order, tier2Order)
        );

        assertNotNull(summary);
        assertEquals(2, summary.lateOrdersCount());

        LateOrderSummary late1 = summary.lateOrders().stream()
                .filter(l -> l.orderNumber().equals("ORD-TIER1")).findFirst().orElseThrow();
        assertEquals(4.0, late1.delayHours());
        assertEquals(150.0, late1.penaltyRatePerHour());
        assertEquals(600.0, late1.penaltyAmount()); // 4 hrs * ₹150/hr = ₹600

        LateOrderSummary late2 = summary.lateOrders().stream()
                .filter(l -> l.orderNumber().equals("ORD-TIER2")).findFirst().orElseThrow();
        assertEquals(4.0, late2.delayHours());
        assertEquals(75.0, late2.penaltyRatePerHour());
        assertEquals(300.0, late2.penaltyAmount()); // 4 hrs * ₹75/hr = ₹300

        assertEquals(900.0, summary.totalPenaltyCost()); // 600 + 300 = 900
    }

    @Test
    void calculateCostSummary_breakdownCausesOvertimeAndPenalty_calculatesDisruptionCost() {
        LocalDate today = LocalDate.now();
        LocalDateTime baseTime = LocalDateTime.of(today, java.time.LocalTime.of(8, 0));
        LocalDateTime dueDate = baseTime.plusHours(8); // Due at 16:00

        Order order = new Order("ORD-IMPACT", tier1Customer, 10, "SHAFT", dueDate, "OPEN");
        ReflectionTestUtils.setField(order, "id", 201L);

        // Op delayed due to breakdown, running from 08:00 to 18:00 (600 mins = 10 hrs)
        // Overtime = 2 hrs (₹1000)
        // Completion = 18:00 (2 hrs late past 16:00 => 2 hrs * ₹150 = ₹300)
        Operation op = new Operation(order, 1, "TURNING", 600, "TURNING");
        ScheduleResult res = new ScheduleResult(op, lathe1, operatorRavi, baseTime, baseTime.plusMinutes(600));

        CostImpactSummary summary = costCalculationService.calculateCostSummary(List.of(res), List.of(order));

        assertNotNull(summary);
        assertEquals(2.0, summary.totalOvertimeHours());
        assertEquals(1000.0, summary.totalOvertimeCost());
        assertEquals(1, summary.lateOrdersCount());
        assertEquals(300.0, summary.totalPenaltyCost());
        assertEquals(1300.0, summary.totalCost()); // Overtime (1000) + Penalty (300) = 1300
    }

    @Test
    void calculateCostSummary_beforeVsAfterComparison_computesNetImpact() {
        LocalDate today = LocalDate.now();
        LocalDateTime baseTime = LocalDateTime.of(today, java.time.LocalTime.of(8, 0));
        LocalDateTime dueDate = baseTime.plusHours(8);

        Order order = new Order("ORD-COMPARE", tier1Customer, 10, "SHAFT", dueDate, "OPEN");
        ReflectionTestUtils.setField(order, "id", 301L);

        // Baseline (Before): On time, 400 mins work (0 OT, 0 Penalty => Total = 0)
        Operation beforeOp = new Operation(order, 1, "TURNING", 400, "TURNING");
        ScheduleResult beforeRes = new ScheduleResult(beforeOp, lathe1, operatorRavi, baseTime, baseTime.plusMinutes(400));
        CostImpactSummary beforeSummary = costCalculationService.calculateCostSummary(List.of(beforeRes), List.of(order));

        // Replanned (After): Delayed by breakdown, 540 mins work (60 mins OT = ₹500), finishes 2 hrs late (2 * ₹150 = ₹300)
        Operation afterOp = new Operation(order, 1, "TURNING", 540, "TURNING");
        ScheduleResult afterRes = new ScheduleResult(afterOp, lathe1, operatorRavi, baseTime.plusHours(2), baseTime.plusHours(2).plusMinutes(540));
        CostImpactSummary afterSummary = costCalculationService.calculateCostSummary(List.of(afterRes), List.of(order));

        assertEquals(0.0, beforeSummary.totalCost());
        assertEquals(500.0, afterSummary.totalOvertimeCost()); // 1 hr OT = ₹500
        assertEquals(450.0, afterSummary.totalPenaltyCost()); // 3 hrs late (11:00 to 19:00 vs due 16:00 => 3 hrs * 150 = 450)
        assertEquals(950.0, afterSummary.totalCost());

        double netCostImpact = afterSummary.totalCost() - beforeSummary.totalCost();
        assertEquals(950.0, netCostImpact);
    }
}
