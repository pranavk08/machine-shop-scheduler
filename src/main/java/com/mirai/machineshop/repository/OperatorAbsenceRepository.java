package com.mirai.machineshop.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.mirai.machineshop.entity.OperatorAbsence;

@Repository
public interface OperatorAbsenceRepository extends JpaRepository<OperatorAbsence, Long> {

    List<OperatorAbsence> findByOperatorId(Long operatorId);

    List<OperatorAbsence> findByOperatorOperatorCodeIgnoreCase(String operatorCode);
}