package com.mirai.machineshop.repository;

import com.mirai.machineshop.entity.OperatorShift;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface OperatorShiftRepository
        extends JpaRepository<OperatorShift, Long> {

    List<OperatorShift> findByOperatorIdAndWorkDateAndAvailableTrue(
            Long operatorId,
            LocalDate workDate);
}