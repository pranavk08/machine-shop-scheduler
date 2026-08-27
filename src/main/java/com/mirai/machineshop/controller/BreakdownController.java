package com.mirai.machineshop.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mirai.machineshop.entity.Breakdown;
import com.mirai.machineshop.repository.BreakdownRepository;

@RestController
@RequestMapping("/api/breakdowns")
public class BreakdownController {

    private final BreakdownRepository breakdownRepository;

    public BreakdownController(BreakdownRepository breakdownRepository) {
        this.breakdownRepository = breakdownRepository;
    }

    @GetMapping
    public List<Breakdown> getAllBreakdowns() {
        return breakdownRepository.findAll();
    }
}