package com.mirai.machineshop.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mirai.machineshop.entity.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByStatusIgnoreCase(String status);

}