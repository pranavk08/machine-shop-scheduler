package com.mirai.machineshop.scheduler;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

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

    public SchedulerService(
            MachineCapabilityRepository machineCapabilityRepository,
            OperationRepository operationRepository,
            OrderRepository orderRepository,
            OperatorSkillRepository operatorSkillRepository,
            OperatorShiftRepository operatorShiftRepository,
            ChangeoverRepository changeoverRepository,
            BreakdownRepository breakdownRepository) {

        this.machineCapabilityRepository = machineCapabilityRepository;
        this.operationRepository = operationRepository;
        this.orderRepository = orderRepository;
        this.operatorSkillRepository = operatorSkillRepository;
        this.operatorShiftRepository = operatorShiftRepository;
        this.changeoverRepository = changeoverRepository;
        this.breakdownRepository = breakdownRepository;
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
            LocalDateTime endTime) {

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

        boolean noBreakdownConflict =
                isMachineAvailableDuringBreakdown(
                        machine,
                        startTime,
                        endTime);

        return noBookingConflict
                && noBreakdownConflict;
    }

    private boolean isMachineAvailableDuringBreakdown(
            Machine machine,
            LocalDateTime startTime,
            LocalDateTime endTime) {

        List<Breakdown> breakdowns =
                breakdownRepository.findByMachineId(
                        machine.getId());

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
            int processingMinutes) {

        List<Machine> machines =
                findCapableMachines(requiredMachineType);

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
                        candidateEndTime)) {

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
                                candidateStartTime);

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
            LocalDateTime currentTime) {

        return schedulingState.machineBookings.stream()
                .filter(booking ->
                        booking.getMachine().getId()
                                .equals(machine.getId()))
                .filter(booking ->
                        booking.getEndTime()
                                .isAfter(currentTime))
                .map(MachineBooking::getEndTime)
                .min(LocalDateTime::compareTo)
                .orElse(currentTime);
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
                findQualifiedOperators(requiredSkill);

        List<Operator> availableOperators = new ArrayList<>();

        for (Operator operator : qualifiedOperators) {

            if (!operator.isAvailable()) {
                continue;
            }

            LocalDate queryDate = (date != null) ? date : startTime.toLocalDate();

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

            List<OperatorShift> allCandidateShifts = new ArrayList<>();
            if (currentDayShifts != null) {
                allCandidateShifts.addAll(currentDayShifts);
            }
            if (previousDayShifts != null) {
                allCandidateShifts.addAll(previousDayShifts);
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
            int processingMinutes) {

        LocalDateTime candidateStartTime = requestedStartTime;

        for (int i = 0; i < MAX_SEARCH_ATTEMPTS; i++) {

            MachineAvailability machineAvailability =
                    findMachineAvailableAt(
                            schedulingState,
                            machineType,
                            partFamily,
                            candidateStartTime,
                            processingMinutes);

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
                new SchedulingState());
    }

    private List<ScheduleResult> scheduleOrder(
            Long orderId,
            LocalDateTime schedulingStartTime,
            SchedulingState schedulingState) {

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
                            operation.getProcessingTimeMinutes());

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

        SchedulingState schedulingState =
                new SchedulingState();

        LocalDateTime schedulingStartTime = LocalDateTime.now();

        List<Order> openOrders =
                orderRepository.findByStatusIgnoreCase("OPEN");

        if (openOrders.isEmpty()) {
            throw new SchedulingUnavailableException(
                    "No open orders found.");
        }

        List<ScheduleResult> completeSchedule =
                new ArrayList<>();

        openOrders.sort((a, b) ->
                a.getDueDate().compareTo(b.getDueDate()));

        for (Order order : openOrders) {

            List<ScheduleResult> orderSchedule =
                    scheduleOrder(
                            order.getId(),
                            schedulingStartTime,
                            schedulingState);

            completeSchedule.addAll(orderSchedule);
        }

        return completeSchedule;
    }

    private static class SchedulingState {

        private final List<MachineBooking> machineBookings =
                new ArrayList<>();

        private final List<OperatorBooking> operatorBookings =
                new ArrayList<>();

        private final Map<String, Integer> changeoverCache =
                new HashMap<>();
    }
}
