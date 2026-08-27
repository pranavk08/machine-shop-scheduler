package com.mirai.machineshop.repository;

import com.mirai.machineshop.entity.MachineCapability;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MachineCapabilityRepository
        extends JpaRepository<MachineCapability, Long> {

    List<MachineCapability> findByCapabilityIgnoreCase(
            String capability);
}