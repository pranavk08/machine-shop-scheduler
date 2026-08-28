package com.mirai.machineshop.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mirai.machineshop.dto.BreakdownRequest;
import com.mirai.machineshop.entity.Breakdown;
import com.mirai.machineshop.service.BreakdownService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/breakdowns")
@Validated
public class BreakdownController {

    private final BreakdownService breakdownService;

    public BreakdownController(BreakdownService breakdownService) {
        this.breakdownService = breakdownService;
    }

    @GetMapping
    public List<Breakdown> getAllBreakdowns() {
        return breakdownService.getAllBreakdowns();
    }

    @PostMapping
    public ResponseEntity<Breakdown> createBreakdown(@Valid @RequestBody BreakdownRequest request) {
        Breakdown created = breakdownService.createBreakdown(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}