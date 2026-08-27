package com.mirai.machineshop.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mirai.machineshop.entity.Machine;
import com.mirai.machineshop.entity.Operator;
import com.mirai.machineshop.scheduler.ScheduleResult;
import com.mirai.machineshop.scheduler.SchedulerService;

@RestController
@RequestMapping("/api/scheduler")
public class SchedulerController {

    private final SchedulerService schedulerService;

    public SchedulerController(SchedulerService schedulerService) {
        this.schedulerService = schedulerService;
    }

    @GetMapping("/machines/{type}")
    public List<Machine> findMachines(
            @PathVariable String type) {

        return schedulerService.findCapableMachines(type);
    }

    @GetMapping("/available-machine/{type}")
    public Machine findAvailableMachine(
            @PathVariable String type) {

        return schedulerService.findAvailableMachine(type);
    }
    
    @GetMapping("/operation/{operationId}/machine")
    public Machine findMachineForOperation(
            @PathVariable Long operationId) {

        return schedulerService
                .findMachineForOperation(operationId);
    }
    
    @GetMapping("/operation/{operationId}/schedule")
    public ScheduleResult scheduleOperation(
            @PathVariable Long operationId) {

        return schedulerService.scheduleOperation(operationId);
    }
    
    @GetMapping("/order/{orderId}/schedule")
    public List<ScheduleResult> scheduleOrder(
            @PathVariable Long orderId) {

    	return schedulerService.scheduleOrder(
    	        orderId,
    	        LocalDateTime.now()
    	);
    }
    
    @GetMapping("/orders/schedule")
    public List<ScheduleResult> scheduleAllOpenOrders() {

        return schedulerService.scheduleAllOpenOrders();
    }
    
    @GetMapping("/operators/skill/{skill}")
    public List<Operator> findQualifiedOperators(
            @PathVariable String skill) {

        return schedulerService.findQualifiedOperators(skill);
    }
    
    @GetMapping("/operators/available")
    public List<Operator> findAvailableOperators(
            @RequestParam String skill,
            @RequestParam String date,
            @RequestParam String start,
            @RequestParam String end) {

        return schedulerService.findAvailableOperators(
                skill,
                LocalDate.parse(date),
                LocalDateTime.parse(start),
                LocalDateTime.parse(end)
        );
    }
    
   
}

