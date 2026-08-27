package com.mirai.machineshop.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mirai.machineshop.entity.Changeover;

public interface ChangeoverRepository
        extends JpaRepository<Changeover, Long> {

    Optional<Changeover> findByMachineIdAndFromPartFamilyIgnoreCaseAndToPartFamilyIgnoreCase(
            Long machineId,
            String fromPartFamily,
            String toPartFamily);
}