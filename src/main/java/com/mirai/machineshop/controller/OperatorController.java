package com.mirai.machineshop.controller;

import com.mirai.machineshop.dto.OperatorRequest;
import com.mirai.machineshop.entity.Operator;
import com.mirai.machineshop.service.OperatorService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

import java.util.List;

@RestController
@RequestMapping("/api/operators")
@Validated
public class OperatorController {

    private final OperatorService operatorService;

    public OperatorController(OperatorService operatorService) {
        this.operatorService = operatorService;
    }

    @PostMapping
    public Operator createOperator(@Valid @RequestBody OperatorRequest request) {
        return operatorService.createOperator(request);
    }

    @GetMapping
    public List<Operator> getAllOperators() {
        return operatorService.getAllOperators();
    }

    @GetMapping("/{id}")
    public Operator getOperatorById(@Positive @PathVariable Long id) {
        return operatorService.getOperatorById(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOperator(@Positive @PathVariable Long id) {
        operatorService.deleteOperator(id);
        return ResponseEntity.noContent().build();
    }
}
