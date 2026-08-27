package com.mirai.machineshop.repository;

import com.mirai.machineshop.entity.Operation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OperationRepository
        extends JpaRepository<Operation, Long> {
}