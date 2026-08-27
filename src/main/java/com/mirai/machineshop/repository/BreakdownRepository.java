package com.mirai.machineshop.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mirai.machineshop.entity.Breakdown;

public interface BreakdownRepository
        extends JpaRepository<Breakdown, Long> {

    List<Breakdown> findByMachineId(Long machineId);

    List<Breakdown> findByStartTimeLessThanAndEndTimeGreaterThan(
            LocalDateTime endTime,
            LocalDateTime startTime);
}