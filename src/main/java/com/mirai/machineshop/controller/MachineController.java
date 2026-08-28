package com.mirai.machineshop.controller;

import com.mirai.machineshop.dto.MachineRequest;
import com.mirai.machineshop.entity.Machine;
import com.mirai.machineshop.service.MachineService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

import java.util.List;

@RestController
@RequestMapping("/api/machines")
@Validated
public class MachineController {

    private final MachineService machineService;

    public MachineController(MachineService machineService) {
        this.machineService = machineService;
    }

    @PostMapping
    public Machine createMachine(@Valid @RequestBody MachineRequest request) {
        return machineService.createMachine(request);
    }

    @GetMapping
    public List<Machine> getAllMachines() {
        return machineService.getAllMachines();
    }

    @GetMapping("/{id}")
    public Machine getMachineById(@Positive @PathVariable Long id) {
        return machineService.getMachineById(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMachine(@Positive @PathVariable Long id) {
        machineService.deleteMachine(id);
        return ResponseEntity.noContent().build();
    }
}

