package com.mirai.machineshop.scheduler;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.mirai.machineshop.dto.OperationScheduleDelta;
import com.mirai.machineshop.dto.ReplanResultResponse;
import com.mirai.machineshop.entity.Breakdown;
import com.mirai.machineshop.entity.Changeover;
import com.mirai.machineshop.entity.Machine;
import com.mirai.machineshop.entity.MachineCapability;
import com.mirai.machineshop.entity.Operation;
import com.mirai.machineshop.entity.Operator;
import com.mirai.machineshop.entity.OperatorShift;
import com.mirai.machineshop.entity.OperatorSkill;
import com.mirai.machineshop.entity.Order;
import com.mirai.machineshop.exception.ResourceNotFoundException;
import com.mirai.machineshop.exception.SchedulingUnavailableException;
import com.mirai.machineshop.repository.BreakdownRepository;
import com.mirai.machineshop.repository.ChangeoverRepository;
import com.mirai.machineshop.repository.MachineCapabilityRepository;
import com.mirai.machineshop.repository.OperationRepository;
import com.mirai.machineshop.repository.OperatorShiftRepository;
import com.mirai.machineshop.repository.OperatorSkillRepository;
import com.mirai.machineshop.repository.OrderRepository;
import com.mirai.machineshop.service.CostCalculationService;

@Service
public class SchedulerService {

    private static final int SEARCH_HORIZON_DAYS = 30;
    private static final int TIME_SLOT_MINUTES = 30;
    private static final int MAX_SEARCH_ATTEMPTS =
            (SEARCH_HORIZON_DAYS * 24 * 60) / TIME_SLOT_MINUTES;

    private final MachineCapabilityRepository machineCapabilityRepository;
    private final OperationRepository operationRepository;
    private final OrderRepository orderRepository;
    private final OperatorSkillRepository operatorSkillRepository;
    private final OperatorShiftRepository operatorShiftRepository;
    private final ChangeoverRepository changeoverRepository;
    private final BreakdownRepository breakdownRepository;
    private final CostCalculationService costCalculationService;

    @Autowired
    public SchedulerService(
            MachineCapabilityRepository machineCapabilityRepository,
            OperationRepository operationRepository,
            OrderRepository orderRepository,
            OperatorSkillRepository operatorSkillRepository,
            OperatorShiftRepository operatorShiftRepository,
            ChangeoverRepository changeoverRepository,
            BreakdownRepository breakdownRepository,
            CostCalculationService costCalculationService) {

        this.machineCapabilityRepository = machineCapabilityRepository;
        this.operationRepository = operationRepository;
        this.orderRepository = orderRepository;
        this.operatorSkillRepository = operatorSkillRepository;
        this.operatorShiftRepository = operatorShiftRepository;
        this.changeoverRepository = changeoverRepository;
        this.breakdownRepository = breakdownRepository;
        this.costCalculationService = costCalculationService;
    }

    public SchedulerService(
            MachineCapabilityRepository machineCapabilityRepository,
            OperationRepository operationRepository,
            OrderRepository orderRepository,
            OperatorSkillRepository operatorSkillRepository,
            OperatorShiftRepository operatorShiftRepository,
            ChangeoverRepository changeoverRepository,
            BreakdownRepository breakdownRepository) {

        this(machineCapabilityRepository, operationRepository, orderRepository,
                operatorSkillRepository, operatorShiftRepository, changeoverRepository,
                breakdownRepository,
                new com.mirai.machineshop.service.CostCalculationService(
                        changeoverRepository, 480, 500.0, 150.0, 75.0, 300.0));
    }

    private boolean isShiftCoveringOperation(
            OperatorShift operatorShift,
            LocalDateTime startTime,
            LocalDateTime endTime) {

        if (operatorShift == null || operatorShift.getShift() == null) {
            return false;
        }

        LocalTime shiftStart = operatorShift.getShift().getStartTime();
        LocalTime shiftEnd = operatorShift.getShift().getEndTime();

        LocalDateTime shiftStartDateTime =
                LocalDateTime.of(operatorShift.getWorkDate(), shiftStart);

        LocalDateTime shiftEndDateTime;
        if (!shiftEnd.isAfter(shiftStart)) {
            shiftEndDateTime =
                    LocalDateTime.of(operatorShift.getWorkDate().plusDays(1), shiftEnd);
        } else {
            shiftEndDateTime =
                    LocalDateTime.of(operatorShift.getWorkDate(), shiftEnd);
        }

        return !startTime.isBefore(shiftStartDateTime)
                && !endTime.isAfter(shiftEndDateTime);
    }

    private boolean isMachineFree(
            SchedulingState schedulingState,
            Machine machine,
            LocalDateTime startTime,
            LocalDateTime endTime,
            boolean checkBreakdowns) {

        boolean noBookingConflict =
                schedulingState.machineBookings.stream()
                        .filter(booking ->
                                booking.getMachine().getId()
                                        .equals(machine.getId()))
                        .noneMatch(booking ->
                                startTime.isBefore(
                                        booking.getEndTime())
                                && endTime.isAfter(
                                        booking.getStartTime()));

        boolean noBreakdownConflict = !checkBreakdowns
                || isMachineAvailableDuringBreakdown(
                        schedulingState,
                        machine,
                        startTime,
                        endTime);

        return noBookingConflict
                && noBreakdownConflict;
    }

    private boolean isMachineAvailableDuringBreakdown(
            SchedulingState schedulingState,
            Machine machine,
            LocalDateTime startTime,
            LocalDateTime endTime) {

        List<Breakdown> breakdowns;
        if (schedulingState != null && !schedulingState.breakdownsCache.isEmpty()) {
            breakdowns = schedulingState.breakdownsCache.getOrDefault(machine.getId(), List.of());
        } else {
            breakdowns = breakdownRepository.findByMachineId(machine.getId());
        }

        if (breakdowns == null || breakdowns.isEmpty()) {
            return true;
        }

        return breakdowns.stream()
                .noneMatch(breakdown ->
                        startTime.isBefore(
                                breakdown.getEndTime())
                        && endTime.isAfter(
                                breakdown.getStartTime()));
    }

    private boolean isOperatorFree(
            SchedulingState schedulingState,
            Operator operator,
            LocalDateTime startTime,
            LocalDateTime endTime) {

        return schedulingState.operatorBookings.stream()
                .filter(booking ->
                        booking.getOperator().getId()
                                .equals(operator.getId()))
                .noneMatch(booking ->
                        startTime.isBefore(booking.getEndTime())
                                && endTime.isAfter(
                                        booking.getStartTime()));
    }

    private MachineAvailability findMachineAvailableAt(
            SchedulingState schedulingState,
            String requiredMachineType,
            String partFamily,
            LocalDateTime requestedStartTime,
            int processingMinutes,
            boolean checkBreakdowns) {

        List<Machine> machines =
                findCapableMachines(schedulingState, requiredMachineType);

        Machine bestMachine = null;
        LocalDateTime earliestStartTime = null;
        LocalDateTime earliestOperationStartTime = null;

        for (Machine machine : machines) {

            if (!machine.isAvailable()) {
                continue;
            }

            LocalDateTime candidateStartTime = requestedStartTime;

            String previousPartFamily =
                    getPreviousPartFamily(
                            schedulingState,
                            machine,
                            candidateStartTime);

            int changeoverMinutes =
                    getChangeoverMinutes(
                            schedulingState,
                            machine,
                            previousPartFamily,
                            partFamily);

            for (int attempt = 0; attempt < MAX_SEARCH_ATTEMPTS; attempt++) {

                LocalDateTime operationStartTime =
                        candidateStartTime.plusMinutes(
                                changeoverMinutes);

                LocalDateTime candidateEndTime =
                        operationStartTime.plusMinutes(
                                processingMinutes);

                if (isMachineFree(
                        schedulingState,
                        machine,
                        candidateStartTime,
                        candidateEndTime,
                        checkBreakdowns)) {

                    if (earliestStartTime == null
                            || candidateStartTime.isBefore(
                                    earliestStartTime)) {

                        bestMachine = machine;
                        earliestStartTime = candidateStartTime;
                        earliestOperationStartTime = operationStartTime;
                    }

                    break;
                }

                LocalDateTime nextTime =
                        getNextMachineFreeTime(
                                schedulingState,
                                machine,
                                candidateStartTime,
                                checkBreakdowns);

                // Safety check: make sure the search moves forward
                if (!nextTime.isAfter(candidateStartTime)) {
                    candidateStartTime =
                            candidateStartTime.plusMinutes(30);
                } else {
                    candidateStartTime = nextTime;
                }

                previousPartFamily =
                        getPreviousPartFamily(
                                schedulingState,
                                machine,
                                candidateStartTime);

                changeoverMinutes =
                        getChangeoverMinutes(
                                schedulingState,
                                machine,
                                previousPartFamily,
                                partFamily);
            }
        }

        if (bestMachine == null) {
            return null;
        }

        return new MachineAvailability(
                bestMachine,
                null,
                earliestStartTime,
                earliestOperationStartTime);
    }

    private LocalDateTime getNextMachineFreeTime(
            SchedulingState schedulingState,
            Machine machine,
            LocalDateTime currentTime,
            boolean checkBreakdowns) {

        LocalDateTime nextBookingEnd = schedulingState.machineBookings.stream()
                .filter(booking ->
                        booking.getMachine().getId()
                                .equals(machine.getId()))
                .filter(booking ->
                        booking.getEndTime()
                                .isAfter(currentTime))
                .map(MachineBooking::getEndTime)
                .min(LocalDateTime::compareTo)
                .orElse(currentTime);

        if (!checkBreakdowns) {
            return nextBookingEnd;
        }

        List<Breakdown> breakdowns;
        if (schedulingState != null && !schedulingState.breakdownsCache.isEmpty()) {
            breakdowns = schedulingState.breakdownsCache.getOrDefault(machine.getId(), List.of());
        } else {
            breakdowns = breakdownRepository.findByMachineId(machine.getId());
        }

        LocalDateTime nextBreakdownEnd = (breakdowns != null)
                ? breakdowns.stream()
                        .filter(breakdown ->
                                breakdown.getEndTime()
                                        .isAfter(currentTime))
                        .map(Breakdown::getEndTime)
                        .min(LocalDateTime::compareTo)
                        .orElse(currentTime)
                : currentTime;

        if (nextBookingEnd.isAfter(currentTime) && nextBreakdownEnd.isAfter(currentTime)) {
            return nextBookingEnd.isBefore(nextBreakdownEnd) ? nextBookingEnd : nextBreakdownEnd;
        } else if (nextBookingEnd.isAfter(currentTime)) {
            return nextBookingEnd;
        } else {
            return nextBreakdownEnd;
        }
    }

    private String getPreviousPartFamily(
            SchedulingState schedulingState,
            Machine machine,
            LocalDateTime currentTime) {

        return schedulingState.machineBookings.stream()
                .filter(booking ->
                        booking.getMachine().getId()
                                .equals(machine.getId()))
                .filter(booking ->
                        !booking.getEndTime()
                                .isAfter(currentTime))
                .max((a, b) ->
                        a.getEndTime()
                                .compareTo(b.getEndTime()))
                .map(MachineBooking::getPartFamily)
                .orElse(null);
    }

    public List<Machine> findCapableMachines(
            String requiredMachineType) {
        return findCapableMachines(null, requiredMachineType);
    }

    private List<Machine> findCapableMachines(
            SchedulingState schedulingState,
            String requiredMachineType) {

        if (requiredMachineType == null) {
            return List.of();
        }

        if (schedulingState != null && !schedulingState.capableMachinesCache.isEmpty()) {
            return schedulingState.capableMachinesCache.getOrDefault(requiredMachineType.toUpperCase(), List.of());
        }

        List<MachineCapability> capabilities =
                machineCapabilityRepository
                        .findByCapabilityIgnoreCase(requiredMachineType);

        return capabilities.stream()
                .map(MachineCapability::getMachine)
                .toList();
    }

    public Machine findAvailableMachine(
            String requiredMachineType) {

        List<Machine> machines =
                findCapableMachines(requiredMachineType);

        return machines.stream()
                .filter(Machine::isAvailable)
                .findFirst()
                .orElse(null);
    }

    public List<Operator> findAvailableOperators(
            String requiredSkill,
            LocalDate date,
            LocalDateTime startTime,
            LocalDateTime endTime) {

        return findAvailableOperators(
                new SchedulingState(),
                requiredSkill,
                date,
                startTime,
                endTime);
    }

    private List<Operator> findAvailableOperators(
            SchedulingState schedulingState,
            String requiredSkill,
            LocalDate date,
            LocalDateTime startTime,
            LocalDateTime endTime) {

        List<Operator> qualifiedOperators =
                findQualifiedOperators(schedulingState, requiredSkill);

        List<Operator> availableOperators = new ArrayList<>();

        for (Operator operator : qualifiedOperators) {

            if (!operator.isAvailable()) {
                continue;
            }

            LocalDate queryDate = (date != null) ? date : startTime.toLocalDate();
            List<OperatorShift> allCandidateShifts = new ArrayList<>();

            if (schedulingState != null && !schedulingState.operatorShiftsCache.isEmpty()) {
                String keyToday = (operator.getId() != null ? operator.getId() : operator.getOperatorCode()) + "#" + queryDate;
                String keyPrev = (operator.getId() != null ? operator.getId() : operator.getOperatorCode()) + "#" + queryDate.minusDays(1);
                List<OperatorShift> todayShifts = schedulingState.operatorShiftsCache.get(keyToday);
                List<OperatorShift> prevShifts = schedulingState.operatorShiftsCache.get(keyPrev);
                if (todayShifts != null) allCandidateShifts.addAll(todayShifts);
                if (prevShifts != null) allCandidateShifts.addAll(prevShifts);
            } else {
                List<OperatorShift> currentDayShifts =
                        operatorShiftRepository
                                .findByOperatorIdAndWorkDateAndAvailableTrue(
                                        operator.getId(),
                                        queryDate);

                List<OperatorShift> previousDayShifts =
                        operatorShiftRepository
                                .findByOperatorIdAndWorkDateAndAvailableTrue(
                                        operator.getId(),
                                        queryDate.minusDays(1));

                if (currentDayShifts != null) {
                    allCandidateShifts.addAll(currentDayShifts);
                }
                if (previousDayShifts != null) {
                    allCandidateShifts.addAll(previousDayShifts);
                }
            }

            boolean worksDuringOperation = allCandidateShifts.stream()
                    .anyMatch(operatorShift ->
                            isShiftCoveringOperation(
                                    operatorShift,
                                    startTime,
                                    endTime));

            if (worksDuringOperation
                    && isOperatorFree(
                            schedulingState,
                            operator,
                            startTime,
                            endTime)) {

                availableOperators.add(operator);
            }
        }

        return availableOperators;
    }

    public List<Operator> findQualifiedOperators(
            String requiredSkill) {
        return findQualifiedOperators(null, requiredSkill);
    }

    private List<Operator> findQualifiedOperators(
            SchedulingState schedulingState,
            String requiredSkill) {

        if (requiredSkill == null) {
            return List.of();
        }

        if (schedulingState != null && !schedulingState.qualifiedOperatorsCache.isEmpty()) {
            return schedulingState.qualifiedOperatorsCache.getOrDefault(requiredSkill.toUpperCase(), List.of());
        }

        List<OperatorSkill> skills =
                operatorSkillRepository
                        .findBySkillNameIgnoreCase(requiredSkill);

        return skills.stream()
                .map(OperatorSkill::getOperator)
                .toList();
    }

    public Machine findMachineForOperation(Long operationId) {

        Operation operation = operationRepository
                .findById(operationId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Operation not found: " + operationId));

        String requiredType =
                operation.getRequiredMachineType();

        return findAvailableMachine(requiredType);
    }

    public ScheduleResult scheduleOperation(Long operationId) {

        Operation operation = operationRepository
                .findById(operationId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Operation not found: " + operationId));

        Machine machine = findAvailableMachine(
                operation.getRequiredMachineType());

        if (machine == null) {
            throw new SchedulingUnavailableException(
                    "No available machine found for: "
                            + operation.getRequiredMachineType());
        }

        LocalDateTime startTime = LocalDateTime.now();

        LocalDateTime endTime = startTime.plusMinutes(
                operation.getProcessingTimeMinutes());

        List<Operator> availableOperators =
                findAvailableOperators(
                        operation.getOperationType(),
                        startTime.toLocalDate(),
                        startTime,
                        endTime);

        if (availableOperators.isEmpty()) {
            throw new SchedulingUnavailableException(
                    "No qualified operator available for operation: "
                            + operation.getOperationType());
        }

        Operator operator = availableOperators.get(0);

        return new ScheduleResult(
                operation,
                machine,
                operator,
                startTime,
                endTime
        );
    }

    private LocalDateTime findEarliestOperatorStartTime(
            SchedulingState schedulingState,
            String requiredSkill,
            LocalDateTime requestedStartTime,
            int processingMinutes) {

        LocalDateTime candidateStartTime = requestedStartTime;

        for (int i = 0; i < MAX_SEARCH_ATTEMPTS; i++) {

            LocalDateTime candidateEndTime =
                    candidateStartTime.plusMinutes(processingMinutes);

            List<Operator> operators =
                    findAvailableOperators(
                            schedulingState,
                            requiredSkill,
                            candidateStartTime.toLocalDate(),
                            candidateStartTime,
                            candidateEndTime);

            if (!operators.isEmpty()) {
                return candidateStartTime;
            }

            candidateStartTime =
                    candidateStartTime.plusMinutes(30);
        }

        return null;
    }

    private MachineAvailability findMachineAndOperatorAvailability(
            SchedulingState schedulingState,
            String machineType,
            String operatorSkill,
            String partFamily,
            LocalDateTime requestedStartTime,
            int processingMinutes,
            boolean checkBreakdowns) {

        LocalDateTime candidateStartTime = requestedStartTime;

        for (int i = 0; i < MAX_SEARCH_ATTEMPTS; i++) {

            MachineAvailability machineAvailability =
                    findMachineAvailableAt(
                            schedulingState,
                            machineType,
                            partFamily,
                            candidateStartTime,
                            processingMinutes,
                            checkBreakdowns);

            if (machineAvailability == null) {
                return null;
            }

            LocalDateTime machineStartTime =
                    machineAvailability.getStartTime();

            LocalDateTime operationStartTime =
                    machineAvailability.getOperationStartTime();

            LocalDateTime operationEndTime =
                    operationStartTime.plusMinutes(
                            processingMinutes);

            List<Operator> operators =
                    findAvailableOperators(
                            schedulingState,
                            operatorSkill,
                            operationStartTime.toLocalDate(),
                            operationStartTime,
                            operationEndTime);

            if (!operators.isEmpty()) {

                Operator operator = operators.get(0);

                return new MachineAvailability(
                        machineAvailability.getMachine(),
                        operator,
                        machineStartTime,
                        operationStartTime);
            }
            candidateStartTime =
                    candidateStartTime.plusMinutes(30);
        }

        return null;
    }

    private int getChangeoverMinutes(
            SchedulingState schedulingState,
            Machine machine,
            String fromPartFamily,
            String toPartFamily) {

        if (fromPartFamily == null) {
            return 0;
        }

        String key =
                machine.getId()
                        + "|"
                        + fromPartFamily.toUpperCase()
                        + "|"
                        + toPartFamily.toUpperCase();

        if (schedulingState.changeoverCache.containsKey(key)) {
            return schedulingState.changeoverCache.get(key);
        }

        int minutes = changeoverRepository
                .findByMachineIdAndFromPartFamilyIgnoreCaseAndToPartFamilyIgnoreCase(
                        machine.getId(),
                        fromPartFamily,
                        toPartFamily)
                .map(Changeover::getChangeoverMinutes)
                .orElse(0);

        schedulingState.changeoverCache.put(key, minutes);

        return minutes;
    }

    public List<ScheduleResult> scheduleOrder(
            Long orderId,
            LocalDateTime schedulingStartTime) {

        return scheduleOrder(
                orderId,
                schedulingStartTime,
                new SchedulingState(),
                true);
    }

    private List<ScheduleResult> scheduleOrder(
            Long orderId,
            LocalDateTime schedulingStartTime,
            SchedulingState schedulingState,
            boolean checkBreakdowns) {

        if (!orderRepository.existsById(orderId)) {
            throw new ResourceNotFoundException("Order not found: " + orderId);
        }

        List<Operation> operations = operationRepository
                .findAll()
                .stream()
                .filter(operation ->
                        operation.getOrder().getId().equals(orderId))
                .sorted((a, b) ->
                        Integer.compare(
                                a.getSequenceNumber(),
                                b.getSequenceNumber()))
                .toList();

        if (operations.isEmpty()) {
            throw new SchedulingUnavailableException(
                    "No operations found for order: " + orderId);
        }

        List<ScheduleResult> schedule = new ArrayList<>();
        LocalDateTime nextAvailableTime = schedulingStartTime;

        for (Operation operation : operations) {

            MachineAvailability availability =
                    findMachineAndOperatorAvailability(
                            schedulingState,
                            operation.getRequiredMachineType(),
                            operation.getOperationType(),
                            operation.getOrder().getPartFamily(),
                            nextAvailableTime,
                            operation.getProcessingTimeMinutes(),
                            checkBreakdowns);

            if (availability == null) {
                throw new SchedulingUnavailableException(
                        "No machine and operator available for operation: "
                                + operation.getOperationType());
            }

            Machine machine = availability.getMachine();
            LocalDateTime machineStartTime = availability.getStartTime();
            LocalDateTime startTime = availability.getOperationStartTime();
            LocalDateTime endTime = startTime.plusMinutes(
                    operation.getProcessingTimeMinutes());
            Operator operator = availability.getOperator();

            if (operator == null) {
                throw new SchedulingUnavailableException(
                        "No qualified operator available for operation: "
                                + operation.getOperationType());
            }

            MachineBooking booking =
                    new MachineBooking(
                            machine,
                            operation.getOrder().getPartFamily(),
                            machineStartTime,
                            endTime);

            schedulingState.machineBookings.add(booking);

            OperatorBooking operatorBooking =
                    new OperatorBooking(
                            operator,
                            startTime,
                            endTime);

            schedulingState.operatorBookings.add(operatorBooking);

            ScheduleResult result =
                    new ScheduleResult(
                            operation,
                            machine,
                            operator,
                            startTime,
                            endTime);

            schedule.add(result);

            nextAvailableTime = endTime;
        }

        return schedule;
    }

    public List<ScheduleResult> scheduleAllOpenOrders() {
        return scheduleAllOpenOrders(SchedulingStrategy.MOST_ON_TIME);
    }

    public List<ScheduleResult> scheduleAllOpenOrders(SchedulingStrategy strategy) {

        List<Order> openOrders =
                new ArrayList<>(orderRepository.findByStatusIgnoreCase("OPEN"));

        if (openOrders.isEmpty()) {
            throw new SchedulingUnavailableException(
                    "No open orders found.");
        }

        List<Operation> allOperations = operationRepository.findAll();
        SchedulingStrategy effectiveStrategy = (strategy != null) ? strategy : SchedulingStrategy.MOST_ON_TIME;
        sortOrdersByStrategy(openOrders, effectiveStrategy, allOperations);

        return generateFullSchedule(
                openOrders,
                LocalDateTime.now(),
                true,
                null,
                null);
    }

    public com.mirai.machineshop.dto.StrategyComparisonResponse compareStrategies() {
        LocalDateTime now = LocalDateTime.now();
        List<Order> openOrders = new ArrayList<>(orderRepository.findByStatusIgnoreCase("OPEN"));
        if (openOrders.isEmpty()) {
            throw new SchedulingUnavailableException("No open orders found.");
        }

        List<com.mirai.machineshop.dto.StrategyEvaluationResult> evaluationResults = new ArrayList<>();

        for (SchedulingStrategy strategy : SchedulingStrategy.values()) {
            List<ScheduleResult> schedule = scheduleAllOpenOrders(strategy);
            com.mirai.machineshop.dto.CostImpactSummary costSummary = costCalculationService != null
                    ? costCalculationService.calculateCostSummary(schedule, openOrders)
                    : new com.mirai.machineshop.dto.CostImpactSummary(0.0, 0.0, 0, 0.0, 0.0, 0.0, List.of(), List.of());

            double durationHours = 0.0;
            if (!schedule.isEmpty()) {
                LocalDateTime firstStart = schedule.stream()
                        .map(ScheduleResult::getStartTime)
                        .min(LocalDateTime::compareTo)
                        .orElse(now);
                LocalDateTime lastEnd = schedule.stream()
                        .map(ScheduleResult::getEndTime)
                        .max(LocalDateTime::compareTo)
                        .orElse(now);
                durationHours = Math.round(java.time.Duration.between(firstStart, lastEnd).toMinutes() / 60.0 * 10.0) / 10.0;
            }

            String displayName;
            String description;
            switch (strategy) {
                case MOST_ON_TIME -> {
                    displayName = "Most On-Time Schedule";
                    description = "Prioritizes Tier-1 OEM customer orders and earliest deadlines to minimize penalty exposure.";
                }
                case CHEAPEST_PRODUCTION -> {
                    displayName = "Cheapest Schedule";
                    description = "Batches compatible part families to eliminate 120-180m changeovers and reduce labor overtime.";
                }
                case MOST_ROBUST -> {
                    displayName = "Most Robust Schedule";
                    description = "Schedules critical bottleneck operations and tight-slack orders early to buffer against disruptions.";
                }
                default -> {
                    displayName = strategy.name();
                    description = "";
                }
            }

            evaluationResults.add(new com.mirai.machineshop.dto.StrategyEvaluationResult(
                    strategy.name(),
                    displayName,
                    description,
                    schedule.size(),
                    durationHours,
                    costSummary
            ));
        }

        // Recommendation rule: Minimum total cost, then minimum late orders, then minimum duration
        com.mirai.machineshop.dto.StrategyEvaluationResult best = evaluationResults.stream()
                .min(Comparator.comparingDouble((com.mirai.machineshop.dto.StrategyEvaluationResult s) -> s.costSummary().totalCost())
                        .thenComparingInt(s -> s.costSummary().lateOrdersCount())
                        .thenComparingDouble(com.mirai.machineshop.dto.StrategyEvaluationResult::totalScheduleDurationHours))
                .orElse(evaluationResults.get(0));

        String recommendationReason = String.format(
                "%s is recommended: Achieves lowest total financial cost of ₹%,.2f with %d late orders and %.1f hrs duration.",
                best.displayName(),
                best.costSummary().totalCost(),
                best.costSummary().lateOrdersCount(),
                best.totalScheduleDurationHours()
        );

        return new com.mirai.machineshop.dto.StrategyComparisonResponse(
                now,
                evaluationResults,
                best.strategy(),
                recommendationReason
        );
    }

    private void sortOrdersByStrategy(
            List<Order> orders,
            SchedulingStrategy strategy,
            List<Operation> allOperations) {

        if (orders == null || orders.size() <= 1) {
            return;
        }

        switch (strategy) {
            case MOST_ON_TIME -> {
                // Tier-1 customer orders first, then earliest due date
                orders.sort(Comparator
                        .comparing((Order o) -> (o.getCustomer() != null && o.getCustomer().getTier() != null
                                && (o.getCustomer().getTier().toUpperCase().contains("1") || o.getCustomer().getTier().equalsIgnoreCase("TIER-1"))) ? 1 : 2)
                        .thenComparing(Order::getDueDate, Comparator.nullsLast(Comparator.naturalOrder())));
            }
            case CHEAPEST_PRODUCTION -> {
                // Group orders by partFamily where practical, then earliest due date
                orders.sort(Comparator
                        .comparing((Order o) -> o.getPartFamily() != null ? o.getPartFamily().toUpperCase() : "")
                        .thenComparing(Order::getDueDate, Comparator.nullsLast(Comparator.naturalOrder())));
            }
            case MOST_ROBUST -> {
                // Prioritize orders containing bottleneck operations/machines (e.g. GRINDING), then minimum slack
                Map<String, Integer> processingMap = new HashMap<>();
                Map<String, Boolean> bottleneckMap = new HashMap<>();

                for (Order order : orders) {
                    String key = (order.getId() != null) ? "ID:" + order.getId() : "NUM:" + order.getOrderNumber();
                    List<Operation> orderOps = allOperations.stream()
                            .filter(op -> isSameOrder(op.getOrder(), order))
                            .toList();

                    int totalProcessing = orderOps.stream()
                            .mapToInt(op -> op.getProcessingTimeMinutes() != null ? op.getProcessingTimeMinutes() : 0)
                            .sum();

                    boolean hasBottleneck = orderOps.stream()
                            .anyMatch(op -> (op.getRequiredMachineType() != null && op.getRequiredMachineType().equalsIgnoreCase("GRINDING"))
                                    || (op.getOperationType() != null && op.getOperationType().equalsIgnoreCase("GRINDING")));

                    processingMap.put(key, totalProcessing);
                    bottleneckMap.put(key, hasBottleneck);
                }

                LocalDateTime now = LocalDateTime.now();

                orders.sort(Comparator
                        .comparing((Order o) -> {
                            String key = (o.getId() != null) ? "ID:" + o.getId() : "NUM:" + o.getOrderNumber();
                            return Boolean.TRUE.equals(bottleneckMap.get(key)) ? 1 : 2;
                        })
                        .thenComparingLong((Order o) -> {
                            String key = (o.getId() != null) ? "ID:" + o.getId() : "NUM:" + o.getOrderNumber();
                            long minutesToDue = o.getDueDate() != null ? Duration.between(now, o.getDueDate()).toMinutes() : Long.MAX_VALUE;
                            int totalProcessing = processingMap.getOrDefault(key, 0);
                            return minutesToDue - totalProcessing; // slack
                        })
                        .thenComparing(Order::getDueDate, Comparator.nullsLast(Comparator.naturalOrder())));
            }
        }
    }

    private String getOperationKey(Operation operation) {
        if (operation == null) {
            return "";
        }
        if (operation.getId() != null) {
            return "ID:" + operation.getId();
        }
        String orderNum = (operation.getOrder() != null && operation.getOrder().getOrderNumber() != null)
                ? operation.getOrder().getOrderNumber()
                : (operation.getOrder() != null && operation.getOrder().getId() != null
                        ? String.valueOf(operation.getOrder().getId())
                        : "UNKNOWN");
        return "ORDER:" + orderNum + "#SEQ:" + operation.getSequenceNumber();
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

    private List<ScheduleResult> generateFullSchedule(
            List<Order> openOrders,
            LocalDateTime schedulingStartTime,
            boolean checkBreakdowns,
            List<ScheduleResult> baselineSchedule,
            LocalDateTime lockBeforeTime) {

        SchedulingState schedulingState = createSchedulingState();
        List<ScheduleResult> completeSchedule = new ArrayList<>();

        Map<String, ScheduleResult> baselineMap = new HashMap<>();
        if (baselineSchedule != null) {
            for (ScheduleResult res : baselineSchedule) {
                baselineMap.put(getOperationKey(res.getOperation()), res);
            }
        }

        for (Order order : openOrders) {

            String orderKey = (order.getId() != null)
                    ? "ID:" + order.getId()
                    : "NUM:" + order.getOrderNumber();

            List<Operation> operations = schedulingState.orderOperationsCache.get(orderKey);
            if (operations == null || operations.isEmpty()) {
                operations = operationRepository
                        .findAll()
                        .stream()
                        .filter(operation ->
                                isSameOrder(operation.getOrder(), order))
                        .sorted((a, b) ->
                                Integer.compare(
                                        a.getSequenceNumber(),
                                        b.getSequenceNumber()))
                        .toList();
            }

            LocalDateTime nextAvailableTime = schedulingStartTime;

            for (Operation operation : operations) {

                ScheduleResult baselineOp = baselineMap.get(getOperationKey(operation));

                if (lockBeforeTime != null && baselineOp != null
                        && !baselineOp.getEndTime().isAfter(lockBeforeTime)) {

                    // Preserve locked operation
                    schedulingState.machineBookings.add(new MachineBooking(
                            baselineOp.getMachine(),
                            operation.getOrder().getPartFamily(),
                            baselineOp.getStartTime(),
                            baselineOp.getEndTime()));

                    schedulingState.operatorBookings.add(new OperatorBooking(
                            baselineOp.getOperator(),
                            baselineOp.getStartTime(),
                            baselineOp.getEndTime()));

                    completeSchedule.add(baselineOp);
                    nextAvailableTime = baselineOp.getEndTime();
                    continue;
                }

                LocalDateTime earliestStart = (lockBeforeTime != null && nextAvailableTime.isBefore(lockBeforeTime))
                        ? lockBeforeTime
                        : nextAvailableTime;

                MachineAvailability availability =
                        findMachineAndOperatorAvailability(
                                schedulingState,
                                operation.getRequiredMachineType(),
                                operation.getOperationType(),
                                operation.getOrder().getPartFamily(),
                                earliestStart,
                                operation.getProcessingTimeMinutes(),
                                checkBreakdowns);

                if (availability == null) {
                    throw new SchedulingUnavailableException(
                            "No machine and operator available for operation: "
                                    + operation.getOperationType());
                }

                Machine machine = availability.getMachine();
                LocalDateTime machineStartTime = availability.getStartTime();
                LocalDateTime startTime = availability.getOperationStartTime();
                LocalDateTime endTime = startTime.plusMinutes(
                        operation.getProcessingTimeMinutes());
                Operator operator = availability.getOperator();

                if (operator == null) {
                    throw new SchedulingUnavailableException(
                            "No qualified operator available for operation: "
                                    + operation.getOperationType());
                }

                MachineBooking booking =
                        new MachineBooking(
                                machine,
                                operation.getOrder().getPartFamily(),
                                machineStartTime,
                                endTime);

                schedulingState.machineBookings.add(booking);

                OperatorBooking operatorBooking =
                        new OperatorBooking(
                                operator,
                                startTime,
                                endTime);

                schedulingState.operatorBookings.add(operatorBooking);

                ScheduleResult result =
                        new ScheduleResult(
                                operation,
                                machine,
                                operator,
                                startTime,
                                endTime);

                completeSchedule.add(result);
                nextAvailableTime = endTime;
            }
        }

        return completeSchedule;
    }

    private void removeBooking(SchedulingState state, ScheduleResult res) {
        if (state == null || res == null) {
            return;
        }
        if (res.getMachine() != null && res.getMachine().getId() != null) {
            state.machineBookings.removeIf(b ->
                    b.getMachine() != null
                            && res.getMachine().getId().equals(b.getMachine().getId())
                            && res.getEndTime().equals(b.getEndTime()));
        }
        if (res.getOperator() != null && res.getOperator().getId() != null) {
            state.operatorBookings.removeIf(b ->
                    b.getOperator() != null
                            && res.getOperator().getId().equals(b.getOperator().getId())
                            && res.getStartTime().equals(b.getStartTime())
                            && res.getEndTime().equals(b.getEndTime()));
        }
    }

    private MachineAvailability findBestRecoverySlot(
            SchedulingState schedulingState,
            Operation operation,
            Machine originalMachine,
            LocalDateTime minStartTime,
            int processingMinutes) {

        List<Machine> capableMachines = findCapableMachines(schedulingState, operation.getRequiredMachineType());
        if (capableMachines == null || capableMachines.isEmpty()) {
            return null;
        }

        List<Machine> sortedCandidates = new ArrayList<>(capableMachines);
        sortedCandidates.sort((m1, m2) -> {
            if (originalMachine != null && m1.getId() != null && m2.getId() != null) {
                if (m1.getId().equals(originalMachine.getId())) return -1;
                if (m2.getId().equals(originalMachine.getId())) return 1;
            }
            return m1.getMachineCode().compareTo(m2.getMachineCode());
        });

        MachineAvailability bestCandidate = null;
        long bestScore = Long.MAX_VALUE;

        for (Machine machine : sortedCandidates) {
            if (!machine.isAvailable()) {
                continue;
            }

            LocalDateTime candidateStart = minStartTime;
            String prevPartFamily = getPreviousPartFamily(schedulingState, machine, candidateStart);
            String targetPartFamily = (operation.getOrder() != null) ? operation.getOrder().getPartFamily() : null;
            int changeoverMinutes = getChangeoverMinutes(schedulingState, machine, prevPartFamily, targetPartFamily);

            for (int attempt = 0; attempt < MAX_SEARCH_ATTEMPTS; attempt++) {
                LocalDateTime opStart = candidateStart.plusMinutes(changeoverMinutes);
                LocalDateTime opEnd = opStart.plusMinutes(processingMinutes);

                if (isMachineFree(schedulingState, machine, candidateStart, opEnd, true)) {
                    List<Operator> availableOperators = findAvailableOperators(
                            schedulingState,
                            operation.getOperationType(),
                            opStart.toLocalDate(),
                            opStart,
                            opEnd);

                    if (!availableOperators.isEmpty()) {
                        Operator operator = availableOperators.get(0);
                        long delayMinutes = Duration.between(minStartTime, opEnd).toMinutes();
                        boolean isDifferentMachine = (originalMachine != null && machine.getId() != null && originalMachine.getId() != null
                                && !machine.getId().equals(originalMachine.getId()));

                        long score = delayMinutes * 10L + changeoverMinutes + (isDifferentMachine ? 5L : 0L);

                        if (score < bestScore) {
                            bestScore = score;
                            bestCandidate = new MachineAvailability(machine, operator, candidateStart, opStart);
                        }
                        break;
                    }
                }

                LocalDateTime nextTime = getNextMachineFreeTime(schedulingState, machine, candidateStart, true);
                if (!nextTime.isAfter(candidateStart)) {
                    candidateStart = candidateStart.plusMinutes(30);
                } else {
                    candidateStart = nextTime;
                }

                prevPartFamily = getPreviousPartFamily(schedulingState, machine, candidateStart);
                changeoverMinutes = getChangeoverMinutes(schedulingState, machine, prevPartFamily, targetPartFamily);
            }
        }

        return bestCandidate;
    }

    private List<ScheduleResult> repairScheduleWithMinimalDisruption(
            List<Order> openOrders,
            List<ScheduleResult> beforeSchedule,
            LocalDateTime lockBeforeTime) {

        if (beforeSchedule == null || beforeSchedule.isEmpty()) {
            return Collections.emptyList();
        }

        SchedulingState schedulingState = createSchedulingState();

        // 1. Index baseline results
        Map<String, ScheduleResult> currentSchedule = new LinkedHashMap<>();
        for (ScheduleResult res : beforeSchedule) {
            currentSchedule.put(getOperationKey(res.getOperation()), res);
        }

        // 2. Identify directly conflicted operations with breakdown
        Set<String> conflictedKeys = new LinkedHashSet<>();
        for (ScheduleResult res : beforeSchedule) {
            if (lockBeforeTime != null && !res.getEndTime().isAfter(lockBeforeTime)) {
                continue;
            }

            boolean availableDuringBreakdown = isMachineAvailableDuringBreakdown(
                    schedulingState,
                    res.getMachine(),
                    res.getStartTime(),
                    res.getEndTime());

            if (!availableDuringBreakdown) {
                conflictedKeys.add(getOperationKey(res.getOperation()));
            }
        }

        if (conflictedKeys.isEmpty()) {
            return new ArrayList<>(beforeSchedule);
        }

        // 3. Populate bookings for all non-conflicted operations
        for (ScheduleResult res : beforeSchedule) {
            String opKey = getOperationKey(res.getOperation());
            if (!conflictedKeys.contains(opKey)) {
                schedulingState.machineBookings.add(new MachineBooking(
                        res.getMachine(),
                        res.getOperation().getOrder() != null ? res.getOperation().getOrder().getPartFamily() : "UNKNOWN",
                        res.getStartTime(),
                        res.getEndTime()));

                if (res.getOperator() != null) {
                    schedulingState.operatorBookings.add(new OperatorBooking(
                            res.getOperator(),
                            res.getStartTime(),
                            res.getEndTime()));
                }
            }
        }

        // 4. Map operations by order sequence
        Map<String, List<ScheduleResult>> orderOpsMap = new LinkedHashMap<>();
        for (ScheduleResult res : beforeSchedule) {
            String orderKey = (res.getOperation().getOrder() != null && res.getOperation().getOrder().getId() != null)
                    ? "ID:" + res.getOperation().getOrder().getId()
                    : (res.getOperation().getOrder() != null && res.getOperation().getOrder().getOrderNumber() != null
                            ? "NUM:" + res.getOperation().getOrder().getOrderNumber()
                            : "KEY:" + getOperationKey(res.getOperation()));
            orderOpsMap.computeIfAbsent(orderKey, k -> new ArrayList<>()).add(res);
        }
        for (List<ScheduleResult> list : orderOpsMap.values()) {
            list.sort(Comparator.comparingInt(r -> r.getOperation().getSequenceNumber()));
        }

        // 5. Process repair queue
        Queue<String> repairQueue = new ArrayDeque<>(conflictedKeys);
        Set<String> inQueue = new HashSet<>(conflictedKeys);

        while (!repairQueue.isEmpty()) {
            String opKey = repairQueue.poll();
            inQueue.remove(opKey);

            ScheduleResult originalRes = currentSchedule.get(opKey);
            if (originalRes == null) {
                continue;
            }

            Operation operation = originalRes.getOperation();
            Order order = operation.getOrder();
            String orderKey = (order != null && order.getId() != null)
                    ? "ID:" + order.getId()
                    : (order != null && order.getOrderNumber() != null
                            ? "NUM:" + order.getOrderNumber()
                            : "KEY:" + opKey);

            // Find predecessor end time
            LocalDateTime predecessorEndTime = null;
            List<ScheduleResult> siblings = orderOpsMap.get(orderKey);
            if (siblings != null) {
                for (ScheduleResult sib : siblings) {
                    if (sib.getOperation().getSequenceNumber() < operation.getSequenceNumber()) {
                        ScheduleResult curSib = currentSchedule.get(getOperationKey(sib.getOperation()));
                        if (curSib != null) {
                            if (predecessorEndTime == null || curSib.getEndTime().isAfter(predecessorEndTime)) {
                                predecessorEndTime = curSib.getEndTime();
                            }
                        }
                    }
                }
            }

            LocalDateTime minStartTime = (lockBeforeTime != null) ? lockBeforeTime : LocalDateTime.MIN;
            if (predecessorEndTime != null && predecessorEndTime.isAfter(minStartTime)) {
                minStartTime = predecessorEndTime;
            }
            if (originalRes.getStartTime().isAfter(minStartTime) && predecessorEndTime == null) {
                minStartTime = originalRes.getStartTime();
            }

            MachineAvailability bestAvailability = findBestRecoverySlot(
                    schedulingState,
                    operation,
                    originalRes.getMachine(),
                    minStartTime,
                    operation.getProcessingTimeMinutes());

            if (bestAvailability == null) {
                bestAvailability = findMachineAndOperatorAvailability(
                        schedulingState,
                        operation.getRequiredMachineType(),
                        operation.getOperationType(),
                        order != null ? order.getPartFamily() : "UNKNOWN",
                        minStartTime,
                        operation.getProcessingTimeMinutes(),
                        true);
            }

            if (bestAvailability != null && bestAvailability.getOperator() != null) {
                Machine machine = bestAvailability.getMachine();
                LocalDateTime machineStartTime = bestAvailability.getStartTime();
                LocalDateTime startTime = bestAvailability.getOperationStartTime();
                LocalDateTime endTime = startTime.plusMinutes(operation.getProcessingTimeMinutes());
                Operator operator = bestAvailability.getOperator();

                schedulingState.machineBookings.add(new MachineBooking(
                        machine,
                        order != null ? order.getPartFamily() : "UNKNOWN",
                        machineStartTime,
                        endTime));

                schedulingState.operatorBookings.add(new OperatorBooking(
                        operator,
                        startTime,
                        endTime));

                ScheduleResult newRes = new ScheduleResult(
                        operation,
                        machine,
                        operator,
                        startTime,
                        endTime);

                currentSchedule.put(opKey, newRes);

                // Cascade A: Precedence to immediate successor in same order
                if (siblings != null) {
                    for (ScheduleResult sib : siblings) {
                        if (sib.getOperation().getSequenceNumber() == operation.getSequenceNumber() + 1) {
                            String succKey = getOperationKey(sib.getOperation());
                            ScheduleResult succRes = currentSchedule.get(succKey);
                            if (succRes != null && succRes.getStartTime().isBefore(endTime)) {
                                removeBooking(schedulingState, succRes);
                                if (inQueue.add(succKey)) {
                                    repairQueue.add(succKey);
                                }
                            }
                        }
                    }
                }

                // Cascade B: Machine overlap push on the assigned machine
                for (ScheduleResult otherRes : new ArrayList<>(currentSchedule.values())) {
                    String otherKey = getOperationKey(otherRes.getOperation());
                    if (!otherKey.equals(opKey) && !inQueue.contains(otherKey)) {
                        if (otherRes.getMachine() != null && machine != null
                                && otherRes.getMachine().getId() != null && machine.getId() != null
                                && otherRes.getMachine().getId().equals(machine.getId())) {
                            if (startTime.isBefore(otherRes.getEndTime()) && endTime.isAfter(otherRes.getStartTime())) {
                                removeBooking(schedulingState, otherRes);
                                if (inQueue.add(otherKey)) {
                                    repairQueue.add(otherKey);
                                }
                            }
                        }
                    }
                }
            }
        }

        List<ScheduleResult> finalSchedule = new ArrayList<>();
        for (Order order : openOrders) {
            String orderKey = (order.getId() != null)
                    ? "ID:" + order.getId()
                    : "NUM:" + order.getOrderNumber();
            List<ScheduleResult> siblings = orderOpsMap.get(orderKey);
            if (siblings != null) {
                for (ScheduleResult r : siblings) {
                    ScheduleResult cur = currentSchedule.get(getOperationKey(r.getOperation()));
                    if (cur != null && !finalSchedule.contains(cur)) {
                        finalSchedule.add(cur);
                    }
                }
            }
        }
        if (finalSchedule.size() < currentSchedule.size()) {
            for (ScheduleResult cur : currentSchedule.values()) {
                if (!finalSchedule.contains(cur)) {
                    finalSchedule.add(cur);
                }
            }
        }

        return finalSchedule;
    }

    public ReplanResultResponse replanSchedule(LocalDateTime replanStartTime) {
        return replanSchedule(null, replanStartTime);
    }

    public ReplanResultResponse replanSchedule(
            LocalDateTime baselineStartTime,
            LocalDateTime replanStartTime) {

        LocalDateTime now = LocalDateTime.now();

        List<Breakdown> allBreakdowns = breakdownRepository.findAll();
        LocalDateTime earliestBreakdownStart = (allBreakdowns != null)
                ? allBreakdowns.stream()
                        .map(Breakdown::getStartTime)
                        .filter(t -> t != null)
                        .min(LocalDateTime::compareTo)
                        .orElse(null)
                : null;

        LocalDateTime effectiveReplanTime = (replanStartTime != null)
                ? replanStartTime
                : (earliestBreakdownStart != null && earliestBreakdownStart.isBefore(now) ? earliestBreakdownStart : now);

        LocalDateTime effectiveBaselineTime = (baselineStartTime != null)
                ? baselineStartTime
                : effectiveReplanTime;

        List<Order> openOrders =
                new ArrayList<>(orderRepository.findByStatusIgnoreCase("OPEN"));

        if (openOrders.isEmpty()) {
            throw new SchedulingUnavailableException(
                    "No open orders found.");
        }

        sortOrdersByStrategy(openOrders, SchedulingStrategy.MOST_ON_TIME, operationRepository.findAll());

        // 1. Generate baseline schedule without breakdown restrictions
        List<ScheduleResult> beforeSchedule =
                generateFullSchedule(openOrders, effectiveBaselineTime, false, null, null);

        // 2. Generate replanned schedule with minimal-disruption local repair
        List<ScheduleResult> afterSchedule =
                repairScheduleWithMinimalDisruption(openOrders, beforeSchedule, effectiveReplanTime);

        // 3. Compute Before vs After impact deltas and summary
        List<OperationScheduleDelta> impactDeltas = new ArrayList<>();
        int operationsMovedCount = 0;
        int machinesReassignedCount = 0;
        int operatorsReassignedCount = 0;
        int ordersDelayedCount = 0;

        Map<String, ScheduleResult> beforeOpMap = new HashMap<>();
        for (ScheduleResult res : beforeSchedule) {
            beforeOpMap.put(getOperationKey(res.getOperation()), res);
        }

        for (ScheduleResult afterRes : afterSchedule) {
            String opKey = getOperationKey(afterRes.getOperation());
            ScheduleResult beforeRes = beforeOpMap.get(opKey);

            if (beforeRes == null) {
                continue;
            }

            boolean machineChanged = !afterRes.getMachine().getId()
                    .equals(beforeRes.getMachine().getId());

            boolean operatorChanged = !afterRes.getOperator().getId()
                    .equals(beforeRes.getOperator().getId());

            boolean timeChanged = !afterRes.getStartTime().equals(beforeRes.getStartTime())
                    || !afterRes.getEndTime().equals(beforeRes.getEndTime());

            long delayMinutes = Math.max(
                    0,
                    Duration.between(beforeRes.getEndTime(), afterRes.getEndTime()).toMinutes());

            if (machineChanged || operatorChanged || timeChanged) {
                operationsMovedCount++;
                if (machineChanged) {
                    machinesReassignedCount++;
                }
                if (operatorChanged) {
                    operatorsReassignedCount++;
                }

                impactDeltas.add(new OperationScheduleDelta(
                        afterRes.getOperation().getOrder().getOrderNumber(),
                        afterRes.getOperation().getSequenceNumber(),
                        afterRes.getOperation().getOperationType(),
                        beforeRes.getMachine().getMachineCode(),
                        afterRes.getMachine().getMachineCode(),
                        beforeRes.getOperator().getName(),
                        afterRes.getOperator().getName(),
                        beforeRes.getStartTime(),
                        afterRes.getStartTime(),
                        beforeRes.getEndTime(),
                        afterRes.getEndTime(),
                        delayMinutes,
                        machineChanged,
                        operatorChanged,
                        timeChanged));
            }
        }

        // Check delayed orders based on final operation completion
        for (Order order : openOrders) {
            ScheduleResult latestBefore = beforeSchedule.stream()
                    .filter(res -> isSameOrder(res.getOperation().getOrder(), order))
                    .max((a, b) -> a.getEndTime().compareTo(b.getEndTime()))
                    .orElse(null);

            ScheduleResult latestAfter = afterSchedule.stream()
                    .filter(res -> isSameOrder(res.getOperation().getOrder(), order))
                    .max((a, b) -> a.getEndTime().compareTo(b.getEndTime()))
                    .orElse(null);

            if (latestBefore != null && latestAfter != null
                    && latestAfter.getEndTime().isAfter(latestBefore.getEndTime())) {
                ordersDelayedCount++;
            }
        }

        com.mirai.machineshop.dto.CostImpactSummary beforeCostSummary = costCalculationService != null
                ? costCalculationService.calculateCostSummary(beforeSchedule, openOrders)
                : null;

        com.mirai.machineshop.dto.CostImpactSummary afterCostSummary = costCalculationService != null
                ? costCalculationService.calculateCostSummary(afterSchedule, openOrders)
                : null;

        double netCostImpact = (beforeCostSummary != null && afterCostSummary != null)
                ? Math.round((afterCostSummary.totalCost() - beforeCostSummary.totalCost()) * 100.0) / 100.0
                : 0.0;

        return new ReplanResultResponse(
                effectiveReplanTime,
                afterSchedule.size(),
                operationsMovedCount,
                ordersDelayedCount,
                machinesReassignedCount,
                operatorsReassignedCount,
                beforeSchedule,
                afterSchedule,
                impactDeltas,
                beforeCostSummary,
                afterCostSummary,
                netCostImpact);
    }

    private SchedulingState createSchedulingState() {
        SchedulingState state = new SchedulingState();

        try {
            List<MachineCapability> capabilities = machineCapabilityRepository.findAll();
            if (capabilities != null) {
                for (MachineCapability mc : capabilities) {
                    if (mc.getCapability() != null && mc.getMachine() != null) {
                        state.capableMachinesCache
                                .computeIfAbsent(mc.getCapability().toUpperCase(), k -> new ArrayList<>())
                                .add(mc.getMachine());
                    }
                }
            }
        } catch (Exception ignored) {
        }

        try {
            List<OperatorSkill> skills = operatorSkillRepository.findAll();
            if (skills != null) {
                for (OperatorSkill os : skills) {
                    if (os.getSkillName() != null && os.getOperator() != null) {
                        state.qualifiedOperatorsCache
                                .computeIfAbsent(os.getSkillName().toUpperCase(), k -> new ArrayList<>())
                                .add(os.getOperator());
                    }
                }
            }
        } catch (Exception ignored) {
        }

        try {
            List<OperatorShift> shifts = operatorShiftRepository.findAll();
            if (shifts != null) {
                for (OperatorShift os : shifts) {
                    if (os.isAvailable() && os.getOperator() != null && os.getWorkDate() != null) {
                        String key = (os.getOperator().getId() != null ? os.getOperator().getId() : os.getOperator().getOperatorCode())
                                + "#" + os.getWorkDate();
                        state.operatorShiftsCache
                                .computeIfAbsent(key, k -> new ArrayList<>())
                                .add(os);
                    }
                }
            }
        } catch (Exception ignored) {
        }

        try {
            List<Breakdown> breakdowns = breakdownRepository.findAll();
            if (breakdowns != null) {
                for (Breakdown b : breakdowns) {
                    if (b.getMachine() != null && b.getMachine().getId() != null) {
                        state.breakdownsCache
                                .computeIfAbsent(b.getMachine().getId(), k -> new ArrayList<>())
                                .add(b);
                    }
                }
            }
        } catch (Exception ignored) {
        }

        try {
            List<Changeover> changeovers = changeoverRepository.findAll();
            if (changeovers != null) {
                for (Changeover c : changeovers) {
                    if (c.getMachine() != null && c.getFromPartFamily() != null && c.getToPartFamily() != null) {
                        String key = c.getMachine().getId() + "|" + c.getFromPartFamily().toUpperCase() + "|" + c.getToPartFamily().toUpperCase();
                        state.changeoverCache.put(key, c.getChangeoverMinutes());
                    }
                }
            }
        } catch (Exception ignored) {
        }

        try {
            List<Operation> allOps = operationRepository.findAll();
            if (allOps != null) {
                for (Operation op : allOps) {
                    if (op.getOrder() != null) {
                        String orderKey = (op.getOrder().getId() != null)
                                ? "ID:" + op.getOrder().getId()
                                : "NUM:" + op.getOrder().getOrderNumber();
                        state.orderOperationsCache
                                .computeIfAbsent(orderKey, k -> new ArrayList<>())
                                .add(op);
                    }
                }
                for (List<Operation> list : state.orderOperationsCache.values()) {
                    list.sort(Comparator.comparingInt(Operation::getSequenceNumber));
                }
            }
        } catch (Exception ignored) {
        }

        return state;
    }

    private static class SchedulingState {

        private final List<MachineBooking> machineBookings =
                new ArrayList<>();

        private final List<OperatorBooking> operatorBookings =
                new ArrayList<>();

        private final Map<String, Integer> changeoverCache =
                new HashMap<>();

        private final Map<String, List<Machine>> capableMachinesCache =
                new HashMap<>();

        private final Map<String, List<Operator>> qualifiedOperatorsCache =
                new HashMap<>();

        private final Map<String, List<OperatorShift>> operatorShiftsCache =
                new HashMap<>();

        private final Map<Long, List<Breakdown>> breakdownsCache =
                new HashMap<>();

        private final Map<String, List<Operation>> orderOperationsCache =
                new HashMap<>();
    }
}
