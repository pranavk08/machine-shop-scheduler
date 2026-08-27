package com.mirai.machineshop.repository;

import com.mirai.machineshop.entity.OperatorSkill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OperatorSkillRepository
        extends JpaRepository<OperatorSkill, Long> {

    List<OperatorSkill> findBySkillNameIgnoreCase(
            String skillName);
}