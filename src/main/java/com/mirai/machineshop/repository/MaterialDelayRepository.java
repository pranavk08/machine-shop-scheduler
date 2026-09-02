package com.mirai.machineshop.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.mirai.machineshop.entity.MaterialDelay;

@Repository
public interface MaterialDelayRepository extends JpaRepository<MaterialDelay, Long> {

    List<MaterialDelay> findByOrderId(Long orderId);
}
