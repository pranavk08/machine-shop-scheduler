package com.mirai.machineshop.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.mirai.machineshop.dto.BreakdownRequest;
import com.mirai.machineshop.entity.Breakdown;
import com.mirai.machineshop.entity.Machine;
import com.mirai.machineshop.exception.InvalidBusinessRequestException;
import com.mirai.machineshop.exception.ResourceNotFoundException;
import com.mirai.machineshop.repository.BreakdownRepository;
import com.mirai.machineshop.repository.MachineRepository;

@Service
public class BreakdownService {

    private final BreakdownRepository breakdownRepository;
    private final MachineRepository machineRepository;

    public BreakdownService(
            BreakdownRepository breakdownRepository,
            MachineRepository machineRepository) {
        this.breakdownRepository = breakdownRepository;
        this.machineRepository = machineRepository;
    }

    public Breakdown createBreakdown(BreakdownRequest request) {
        if (!request.endTime().isAfter(request.startTime())) {
            throw new InvalidBusinessRequestException("endTime must be after startTime.");
        }

        Machine machine = machineRepository.findById(request.machineId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Machine not found: " + request.machineId()));

        Breakdown breakdown = new Breakdown(
                machine,
                request.startTime(),
                request.endTime(),
                request.reason().trim()
        );

        return breakdownRepository.save(breakdown);
    }

    public List<Breakdown> getAllBreakdowns() {
        return breakdownRepository.findAll();
    }
}
