package com.mirai.machineshop.repository;

import com.mirai.machineshop.entity.Machine;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MachineRepository extends JpaRepository<Machine, Long> {

    boolean existsByMachineCodeIgnoreCase(String machineCode);
}
