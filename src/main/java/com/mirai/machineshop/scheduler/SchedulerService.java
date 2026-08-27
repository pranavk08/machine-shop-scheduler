package com.mirai.machineshop.scheduler;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

import com.mirai.machineshop.entity.Breakdown;
import com.mirai.machineshop.entity.Changeover;
import com.mirai.machineshop.entity.Machine;
import com.mirai.machineshop.entity.Order;
import com.mirai.machineshop.entity.MachineCapability;
import com.mirai.machineshop.entity.Operation;
import com.mirai.machineshop.entity.Operator;
import com.mirai.machineshop.entity.OperatorShift;
import com.mirai.machineshop.entity.OperatorSkill;
import com.mirai.machineshop.repository.MachineCapabilityRepository;
import com.mirai.machineshop.repository.OperationRepository;
import com.mirai.machineshop.repository.OperatorShiftRepository;
import com.mirai.machineshop.repository.OperatorSkillRepository;
import com.mirai.machineshop.repository.OrderRepository;
import com.mirai.machineshop.repository.BreakdownRepository;
import com.mirai.machineshop.repository.ChangeoverRepository;






@Service
public class SchedulerService {
	
	private final OperatorSkillRepository operatorSkillRepository;
	
	private final ChangeoverRepository changeoverRepository;
	
	private final BreakdownRepository breakdownRepository;
	
	private final OrderRepository orderRepository;
	
	private final List<MachineBooking> machineBookings =
	        new ArrayList<>();

	private final List<OperatorBooking> operatorBookings =
	        new ArrayList<>();

	private final Map<String, Integer> changeoverCache =
	        new HashMap<>();
	
	private final OperatorShiftRepository operatorShiftRepository;
	

    private final MachineCapabilityRepository machineCapabilityRepository;
    
    private final OperationRepository operationRepository;
    
    private boolean isMachineFree(
            Machine machine,
            LocalDateTime startTime,
            LocalDateTime endTime) {

        boolean noBookingConflict =
                machineBookings.stream()
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
            Operator operator,
            LocalDateTime startTime,
            LocalDateTime endTime) {

        return operatorBookings.stream()
                .filter(booking ->
                        booking.getOperator().getId()
                                .equals(operator.getId()))
                .noneMatch(booking ->
                        startTime.isBefore(booking.getEndTime())
                                && endTime.isAfter(
                                        booking.getStartTime()));
    }
    
    public SchedulerService(
            MachineCapabilityRepository machineCapabilityRepository,
            OperationRepository operationRepository,
            OrderRepository orderRepository,
            OperatorSkillRepository operatorSkillRepository,
            OperatorShiftRepository operatorShiftRepository,
            ChangeoverRepository changeoverRepository,
            BreakdownRepository breakdownRepository) {

        this.machineCapabilityRepository =
                machineCapabilityRepository;

        this.operationRepository =
                operationRepository;
        
        this.orderRepository = orderRepository;

        this.operatorSkillRepository =
                operatorSkillRepository;

        this.operatorShiftRepository =
                operatorShiftRepository;

        this.changeoverRepository =
                changeoverRepository;
        
        this.breakdownRepository = breakdownRepository;
    }
    
    
    
    
    private MachineAvailability findMachineAvailableAt(
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

            LocalDateTime candidateStartTime =
                    requestedStartTime;

            String previousPartFamily =
                    getPreviousPartFamily(
                            machine,
                            candidateStartTime);

            int changeoverMinutes =
                    getChangeoverMinutes(
                            machine,
                            previousPartFamily,
                            partFamily);

            for (int attempt = 0; attempt < 96; attempt++) {

                LocalDateTime operationStartTime =
                        candidateStartTime.plusMinutes(
                                changeoverMinutes);

                LocalDateTime candidateEndTime =
                        operationStartTime.plusMinutes(
                                processingMinutes);
                
                

                if (isMachineFree(
                        machine,
                        candidateStartTime,
                        candidateEndTime)) {

                    if (earliestStartTime == null
                            || candidateStartTime.isBefore(
                                    earliestStartTime)) {

                        bestMachine = machine;

                        earliestStartTime =
                                candidateStartTime;

                        earliestOperationStartTime =
                                operationStartTime;
                    }

                    break;
                }

                LocalDateTime nextTime =
                        getNextMachineFreeTime(
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
                                machine,
                                candidateStartTime);

                changeoverMinutes =
                        getChangeoverMinutes(
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
            Machine machine,
            LocalDateTime currentTime) {

        return machineBookings.stream()
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
            Machine machine,
            LocalDateTime currentTime) {

        return machineBookings.stream()
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

        List<Operator> qualifiedOperators =
                findQualifiedOperators(requiredSkill);

        List<Operator> availableOperators = new ArrayList<>();

        for (Operator operator : qualifiedOperators) {

            List<OperatorShift> shifts =
                    operatorShiftRepository
                            .findByOperatorIdAndWorkDateAndAvailableTrue(
                                    operator.getId(),
                                    date);

            boolean worksDuringOperation = shifts.stream()
                    .anyMatch(operatorShift -> {

                        LocalTime shiftStart =
                                operatorShift.getShift().getStartTime();

                        LocalTime shiftEnd =
                                operatorShift.getShift().getEndTime();

                        LocalTime operationStart =
                                startTime.toLocalTime();

                        LocalTime operationEnd =
                                endTime.toLocalTime();

                        return !operationStart.isBefore(shiftStart)
                                && !operationEnd.isAfter(shiftEnd);
                    });

            if (worksDuringOperation
                    && isOperatorFree(
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
                        new RuntimeException(
                                "Operation not found: " + operationId));

        String requiredType =
                operation.getRequiredMachineType();

        return findAvailableMachine(requiredType);
    }
    
    
    
    public ScheduleResult scheduleOperation(Long operationId) {

        Operation operation = operationRepository
                .findById(operationId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Operation not found: " + operationId));

        Machine machine = findAvailableMachine(
                operation.getRequiredMachineType());

        if (machine == null) {
            throw new RuntimeException(
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
            throw new RuntimeException(
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
            String requiredSkill,
            LocalDateTime requestedStartTime,
            int processingMinutes) {

        LocalDateTime candidateStartTime = requestedStartTime;

        for (int i = 0; i < 48; i++) {

            LocalDateTime candidateEndTime =
                    candidateStartTime.plusMinutes(processingMinutes);

            List<Operator> operators =
                    findAvailableOperators(
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
    		    String machineType,
    	        String operatorSkill,
    	        String partFamily,
    	        LocalDateTime requestedStartTime,
    	        int processingMinutes)  {

        LocalDateTime candidateStartTime = requestedStartTime;

        for (int i = 0; i < 96; i++) {

        	MachineAvailability machineAvailability =
        	        findMachineAvailableAt(
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

        if (changeoverCache.containsKey(key)) {
            return changeoverCache.get(key);
        }

        int minutes = changeoverRepository
                .findByMachineIdAndFromPartFamilyIgnoreCaseAndToPartFamilyIgnoreCase(
                        machine.getId(),
                        fromPartFamily,
                        toPartFamily)
                .map(Changeover::getChangeoverMinutes)
                .orElse(0);

        changeoverCache.put(key, minutes);

        return minutes;
    }
    
    
    
    
    public List<ScheduleResult> scheduleOrder(
            Long orderId,
            LocalDateTime schedulingStartTime) { {

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
            throw new RuntimeException(
                    "No operations found for order: " + orderId);
        }

        List<ScheduleResult> schedule = new ArrayList<>();

        LocalDateTime nextAvailableTime = schedulingStartTime;
        
        

        for (Operation operation : operations) {

            MachineAvailability availability =
                    findMachineAndOperatorAvailability(
                            operation.getRequiredMachineType(),
                            operation.getOperationType(),
                            operation.getOrder().getPartFamily(),
                            nextAvailableTime,
                            operation.getProcessingTimeMinutes());

            if (availability == null) {
                throw new RuntimeException(
                        "No machine and operator available for operation: "
                                + operation.getOperationType());
            }

            Machine machine = availability.getMachine();

            LocalDateTime machineStartTime =
                    availability.getStartTime();

            LocalDateTime startTime =
                    availability.getOperationStartTime();

            LocalDateTime endTime =
                    startTime.plusMinutes(
                            operation.getProcessingTimeMinutes());

            Operator operator =
                    availability.getOperator();

            if (operator == null) {
                throw new RuntimeException(
                        "No qualified operator available for operation: "
                                + operation.getOperationType());
            }

            MachineBooking booking =
                    new MachineBooking(
                            machine,
                            operation.getOrder().getPartFamily(),
                            machineStartTime,
                            endTime);

            machineBookings.add(booking);

            OperatorBooking operatorBooking =
                    new OperatorBooking(
                            operator,
                            startTime,
                            endTime);

            operatorBookings.add(operatorBooking);

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
            }
    
            public List<ScheduleResult> scheduleAllOpenOrders() {

                machineBookings.clear();
                operatorBookings.clear();

                LocalDateTime schedulingStartTime = LocalDateTime.now();

                List<Order> openOrders =
                        orderRepository.findByStatusIgnoreCase("OPEN");

                if (openOrders.isEmpty()) {
                    throw new RuntimeException(
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
                                    schedulingStartTime);

                    completeSchedule.addAll(orderSchedule);
                }

                return completeSchedule;
            }
}