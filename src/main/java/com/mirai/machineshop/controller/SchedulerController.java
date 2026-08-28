package com.mirai.machineshop.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mirai.machineshop.dto.ReplanRequest;
import com.mirai.machineshop.dto.ReplanResultResponse;
import com.mirai.machineshop.entity.Machine;
import com.mirai.machineshop.entity.Operator;
import com.mirai.machineshop.exception.InvalidBusinessRequestException;
import com.mirai.machineshop.scheduler.ScheduleResult;
import com.mirai.machineshop.scheduler.SchedulerService;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

@RestController
@RequestMapping("/api/scheduler")
@Validated
public class SchedulerController {

    private final SchedulerService schedulerService;

    public SchedulerController(SchedulerService schedulerService) {
        this.schedulerService = schedulerService;
    }

    @GetMapping("/machines/{type}")
    public List<Machine> findMachines(
            @NotBlank @PathVariable String type) {

        return schedulerService.findCapableMachines(type);
    }

    @GetMapping("/available-machine/{type}")
    public Machine findAvailableMachine(
            @NotBlank @PathVariable String type) {

        return schedulerService.findAvailableMachine(type);
    }
    
    @GetMapping("/operation/{operationId}/machine")
    public Machine findMachineForOperation(
            @Positive @PathVariable Long operationId) {

        return schedulerService
                .findMachineForOperation(operationId);
    }
    
    @GetMapping("/operation/{operationId}/schedule")
    public ScheduleResult scheduleOperation(
            @Positive @PathVariable Long operationId) {

        return schedulerService.scheduleOperation(operationId);
    }
    
    @GetMapping("/order/{orderId}/schedule")
    public List<ScheduleResult> scheduleOrder(
            @Positive @PathVariable Long orderId) {

    	return schedulerService.scheduleOrder(
    	        orderId,
    	        LocalDateTime.now()
    	);
    }
    
    @GetMapping("/orders/schedule")
    public List<ScheduleResult> scheduleAllOpenOrders(
            @RequestParam(required = false) com.mirai.machineshop.scheduler.SchedulingStrategy strategy) {

        if (strategy == null) {
            return schedulerService.scheduleAllOpenOrders();
        }
        return schedulerService.scheduleAllOpenOrders(strategy);
    }

    @GetMapping("/strategies/compare")
    public com.mirai.machineshop.dto.StrategyComparisonResponse compareStrategies() {
        return schedulerService.compareStrategies();
    }

    @PostMapping("/replan")
    public ReplanResultResponse replanSchedule(
            @RequestBody(required = false) ReplanRequest request) {

        LocalDateTime baselineStartTime = (request != null)
                ? request.baselineStartTime()
                : null;

        LocalDateTime replanStartTime = (request != null && request.replanStartTime() != null)
                ? request.replanStartTime()
                : LocalDateTime.now();

        return schedulerService.replanSchedule(baselineStartTime, replanStartTime);
    }
    
    @GetMapping("/operators/skill/{skill}")
    public List<Operator> findQualifiedOperators(
            @NotBlank @PathVariable String skill) {

        return schedulerService.findQualifiedOperators(skill);
    }
    
    @GetMapping("/operators/available")
    public List<Operator> findAvailableOperators(
            @NotBlank @RequestParam String skill,
            @RequestParam String date,
            @RequestParam String start,
            @RequestParam String end) {

        LocalDate parsedDate;
        LocalDateTime parsedStart;
        LocalDateTime parsedEnd;

        try {
            parsedDate = LocalDate.parse(date);
            parsedStart = LocalDateTime.parse(start);
            parsedEnd = LocalDateTime.parse(end);
        } catch (DateTimeParseException exception) {
            throw new InvalidBusinessRequestException(
                    "date, start, and end must use ISO-8601 format.");
        }

        if (!parsedEnd.isAfter(parsedStart)) {
            throw new InvalidBusinessRequestException(
                    "end must be after start.");
        }

        if (!parsedStart.toLocalDate().equals(parsedDate)) {
            throw new InvalidBusinessRequestException(
                    "date must match the start date.");
        }

        return schedulerService.findAvailableOperators(
                skill,
                parsedDate,
                parsedStart,
                parsedEnd
        );
    }
}
