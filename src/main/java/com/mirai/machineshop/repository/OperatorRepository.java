package com.mirai.machineshop.repository;

import com.mirai.machineshop.entity.Operator;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OperatorRepository extends JpaRepository<Operator, Long> {

}