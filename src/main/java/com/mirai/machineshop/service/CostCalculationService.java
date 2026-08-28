package com.mirai.machineshop.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.mirai.machineshop.dto.CostImpactSummary;
import com.mirai.machineshop.dto.LateOrderSummary;
import com.mirai.machineshop.dto.OperatorOvertimeSummary;
import com.mirai.machineshop.entity.Changeover;
import com.mirai.machineshop.entity.Customer;
import com.mirai.machineshop.entity.Machine;
import com.mirai.machineshop.entity.Operator;
import com.mirai.machineshop.entity.Order;
import com.mirai.machineshop.repository.ChangeoverRepository;
import com.mirai.machineshop.scheduler.ScheduleResult;

@Service
public class CostCalculationService {

    private final ChangeoverRepository changeoverRepository;
    private final int regularShiftCapacityMinutes;
    private final double overtimeHourlyRate;
    private final double tier1PenaltyHourlyRate;
    private final double tier2PenaltyHourlyRate;
    private final double changeoverHourlyRate;

    public CostCalculationService(
            ChangeoverRepository changeoverRepository,
            @Value("${scheduler.cost.regular-shift-capacity-minutes:480}") int regularShiftCapacityMinutes,
            @Value("${scheduler.cost.overtime-hourly-rate:500.0}") double overtimeHourlyRate,
            @Value("${scheduler.cost.tier1-penalty-hourly-rate:150.0}") double tier1PenaltyHourlyRate,
            @Value("${scheduler.cost.tier2-penalty-hourly-rate:75.0}") double tier2PenaltyHourlyRate,
            @Value("${scheduler.cost.changeover-hourly-rate:300.0}") double changeoverHourlyRate) {
        this.changeoverRepository = changeoverRepository;
        this.regularShiftCapacityMinutes = regularShiftCapacityMinutes;
        this.overtimeHourlyRate = overtimeHourlyRate;
        this.tier1PenaltyHourlyRate = tier1PenaltyHourlyRate;
        this.tier2PenaltyHourlyRate = tier2PenaltyHourlyRate;
        this.changeoverHourlyRate = changeoverHourlyRate;
    }

    public CostImpactSummary calculateCostSummary(
            List<ScheduleResult> schedule,
            List<Order> orders) {

        if (schedule == null || schedule.isEmpty()) {
            return new CostImpactSummary(0.0, 0.0, 0, 0.0, 0.0, 0.0, List.of(), List.of());
        }

        // 1. Calculate Operator Overtime
        List<OperatorOvertimeSummary> operatorOvertimes = calculateOperatorOvertimes(schedule);
        double totalOvertimeMinutes = operatorOvertimes.stream()
                .mapToInt(OperatorOvertimeSummary::overtimeMinutes)
                .sum();
        double totalOvertimeHours = totalOvertimeMinutes / 60.0;
        double totalOvertimeCost = operatorOvertimes.stream()
                .mapToDouble(OperatorOvertimeSummary::overtimeCost)
                .sum();

        // 2. Calculate Late Orders & Penalties
        List<LateOrderSummary> lateOrders = calculateLateOrders(schedule, orders);
        int lateOrdersCount = lateOrders.size();
        double totalPenaltyCost = lateOrders.stream()
                .mapToDouble(LateOrderSummary::penaltyAmount)
                .sum();

        // 3. Calculate Changeover / Setup Cost
        double totalWastedChangeoverCost = calculateChangeoverCost(schedule);

        // 4. Total Cost
        double totalCost = totalOvertimeCost + totalPenaltyCost + totalWastedChangeoverCost;

        return new CostImpactSummary(
                roundToTwoDecimals(totalOvertimeHours),
                roundToTwoDecimals(totalOvertimeCost),
                lateOrdersCount,
                roundToTwoDecimals(totalPenaltyCost),
                roundToTwoDecimals(totalWastedChangeoverCost),
                roundToTwoDecimals(totalCost),
                operatorOvertimes,
                lateOrders
        );
    }

    private List<OperatorOvertimeSummary> calculateOperatorOvertimes(List<ScheduleResult> schedule) {
        // Group by operator and date
        Map<String, List<ScheduleResult>> opDateMap = new HashMap<>();

        for (ScheduleResult res : schedule) {
            if (res.getOperator() == null || res.getStartTime() == null) {
                continue;
            }
            LocalDate workDate = res.getStartTime().toLocalDate();
            String key = res.getOperator().getId() != null
                    ? res.getOperator().getId() + "#" + workDate
                    : res.getOperator().getOperatorCode() + "#" + workDate;

            opDateMap.computeIfAbsent(key, k -> new ArrayList<>()).add(res);
        }

        List<OperatorOvertimeSummary> overtimes = new ArrayList<>();

        for (Map.Entry<String, List<ScheduleResult>> entry : opDateMap.entrySet()) {
            List<ScheduleResult> opResults = entry.getValue();
            if (opResults.isEmpty()) {
                continue;
            }

            Operator operator = opResults.get(0).getOperator();
            LocalDate workDate = opResults.get(0).getStartTime().toLocalDate();

            int totalScheduledMinutes = 0;
            for (ScheduleResult res : opResults) {
                if (res.getStartTime() != null && res.getEndTime() != null) {
                    totalScheduledMinutes += (int) Duration.between(res.getStartTime(), res.getEndTime()).toMinutes();
                } else if (res.getOperation() != null && res.getOperation().getProcessingTimeMinutes() != null) {
                    totalScheduledMinutes += res.getOperation().getProcessingTimeMinutes();
                }
            }

            int regularMinutes = Math.min(totalScheduledMinutes, regularShiftCapacityMinutes);
            int overtimeMinutes = Math.max(0, totalScheduledMinutes - regularShiftCapacityMinutes);
            double overtimeCost = (overtimeMinutes / 60.0) * overtimeHourlyRate;

            if (overtimeMinutes > 0) {
                overtimes.add(new OperatorOvertimeSummary(
                        operator.getId(),
                        operator.getOperatorCode(),
                        operator.getName(),
                        workDate,
                        totalScheduledMinutes,
                        regularMinutes,
                        overtimeMinutes,
                        roundToTwoDecimals(overtimeCost)
                ));
            }
        }

        overtimes.sort(Comparator.comparing(OperatorOvertimeSummary::workDate)
                .thenComparing(OperatorOvertimeSummary::operatorName));

        return overtimes;
    }

    private List<LateOrderSummary> calculateLateOrders(
            List<ScheduleResult> schedule,
            List<Order> orders) {

        List<LateOrderSummary> lateOrders = new ArrayList<>();

        if (orders == null || orders.isEmpty()) {
            return lateOrders;
        }

        for (Order order : orders) {
            LocalDateTime dueDate = order.getDueDate();
            if (dueDate == null) {
                continue;
            }

            // Find all operations belonging to this order
            List<ScheduleResult> orderResults = schedule.stream()
                    .filter(res -> isSameOrder(res.getOperation() != null ? res.getOperation().getOrder() : null, order))
                    .toList();

            if (orderResults.isEmpty()) {
                continue;
            }

            LocalDateTime completionDate = orderResults.stream()
                    .map(ScheduleResult::getEndTime)
                    .max(LocalDateTime::compareTo)
                    .orElse(null);

            if (completionDate != null && completionDate.isAfter(dueDate)) {
                long lateMinutes = Duration.between(dueDate, completionDate).toMinutes();
                double delayHours = lateMinutes / 60.0;

                Customer customer = order.getCustomer();
                String customerName = customer != null ? customer.getName() : "Unknown";
                String customerTier = customer != null && customer.getTier() != null ? customer.getTier() : "TIER-2";

                double penaltyRate = (customerTier.toUpperCase().contains("1") || customerTier.equalsIgnoreCase("TIER-1"))
                        ? tier1PenaltyHourlyRate
                        : tier2PenaltyHourlyRate;

                double penaltyAmount = delayHours * penaltyRate;

                lateOrders.add(new LateOrderSummary(
                        order.getId(),
                        order.getOrderNumber(),
                        customerName,
                        customerTier,
                        dueDate,
                        completionDate,
                        roundToTwoDecimals(delayHours),
                        roundToTwoDecimals(penaltyRate),
                        roundToTwoDecimals(penaltyAmount)
                ));
            }
        }

        lateOrders.sort(Comparator.comparing(LateOrderSummary::dueDate));
        return lateOrders;
    }

    private double calculateChangeoverCost(List<ScheduleResult> schedule) {
        if (changeoverRepository == null) {
            return 0.0;
        }

        // Group operations by machine and sort by start time
        Map<Long, List<ScheduleResult>> machineScheduleMap = new HashMap<>();

        for (ScheduleResult res : schedule) {
            if (res.getMachine() != null && res.getMachine().getId() != null) {
                machineScheduleMap.computeIfAbsent(res.getMachine().getId(), k -> new ArrayList<>()).add(res);
            }
        }

        int totalChangeoverMinutes = 0;

        for (List<ScheduleResult> machineOps : machineScheduleMap.values()) {
            machineOps.sort(Comparator.comparing(ScheduleResult::getStartTime));

            String previousPartFamily = null;
            Machine machine = machineOps.get(0).getMachine();

            for (ScheduleResult res : machineOps) {
                String currentPartFamily = res.getOperation() != null && res.getOperation().getOrder() != null
                        ? res.getOperation().getOrder().getPartFamily()
                        : null;

                if (previousPartFamily != null && currentPartFamily != null
                        && !previousPartFamily.equalsIgnoreCase(currentPartFamily)) {
                    Optional<Changeover> changeover = changeoverRepository
                            .findByMachineIdAndFromPartFamilyIgnoreCaseAndToPartFamilyIgnoreCase(
                                    machine.getId(),
                                    previousPartFamily,
                                    currentPartFamily);

                    totalChangeoverMinutes += changeover.map(Changeover::getChangeoverMinutes).orElse(120);
                }
                previousPartFamily = currentPartFamily;
            }
        }

        return (totalChangeoverMinutes / 60.0) * changeoverHourlyRate;
    }

    private boolean isSameOrder(Order o1, Order o2) {
        if (o1 == null || o2 == null) {
            return false;
        }
        if (o1.getId() != null && o2.getId() != null) {
            return o1.getId().equals(o2.getId());
        }
        if (o1.getOrderNumber() != null && o2.getOrderNumber() != null) {
            return o1.getOrderNumber().equalsIgnoreCase(o2.getOrderNumber());
        }
        return false;
    }

    private double roundToTwoDecimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    public int getRegularShiftCapacityMinutes() {
        return regularShiftCapacityMinutes;
    }

    public double getOvertimeHourlyRate() {
        return overtimeHourlyRate;
    }

    public double getTier1PenaltyHourlyRate() {
        return tier1PenaltyHourlyRate;
    }

    public double getTier2PenaltyHourlyRate() {
        return tier2PenaltyHourlyRate;
    }

    public double getChangeoverHourlyRate() {
        return changeoverHourlyRate;
    }
}
