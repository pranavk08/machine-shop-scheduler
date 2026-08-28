package com.mirai.machineshop.service;

import com.mirai.machineshop.dto.MachineRequest;
import com.mirai.machineshop.entity.Machine;
import com.mirai.machineshop.exception.DuplicateResourceException;
import com.mirai.machineshop.exception.ResourceNotFoundException;
import com.mirai.machineshop.repository.MachineRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MachineService {

    private final MachineRepository machineRepository;

    public MachineService(MachineRepository machineRepository) {
        this.machineRepository = machineRepository;
    }

    public Machine createMachine(MachineRequest request) {

        if (machineRepository.existsByMachineCodeIgnoreCase(request.machineCode())) {
            throw new DuplicateResourceException(
                    "Machine code already exists: " + request.machineCode());
        }

        Machine machine = new Machine(
                request.machineCode().trim(),
                request.name().trim(),
                request.type().trim());

        if (request.available() != null) {
            machine.setAvailable(request.available());
        }

        return machineRepository.save(machine);
    }

    public List<Machine> getAllMachines() {
        return machineRepository.findAll();
    }

    public Machine getMachineById(Long id) {
        return machineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Machine not found: " + id));
    }

    public void deleteMachine(Long id) {
        if (!machineRepository.existsById(id)) {
            throw new ResourceNotFoundException("Machine not found: " + id);
        }

        machineRepository.deleteById(id);
    }
}
