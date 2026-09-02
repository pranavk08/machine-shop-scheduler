package com.mirai.machineshop.service;

import java.util.List;
import org.springframework.stereotype.Service;

import com.mirai.machineshop.dto.MaterialDelayRequest;
import com.mirai.machineshop.entity.MaterialDelay;
import com.mirai.machineshop.entity.Order;
import com.mirai.machineshop.exception.ResourceNotFoundException;
import com.mirai.machineshop.repository.MaterialDelayRepository;
import com.mirai.machineshop.repository.OrderRepository;

@Service
public class MaterialDelayService {

    private final MaterialDelayRepository materialDelayRepository;
    private final OrderRepository orderRepository;

    public MaterialDelayService(
            MaterialDelayRepository materialDelayRepository,
            OrderRepository orderRepository) {
        this.materialDelayRepository = materialDelayRepository;
        this.orderRepository = orderRepository;
    }

    public MaterialDelay createMaterialDelay(MaterialDelayRequest request) {
        Order order = orderRepository.findById(request.orderId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order not found: " + request.orderId()));

        MaterialDelay materialDelay = new MaterialDelay(
                order,
                request.delayedUntil(),
                request.reason().trim()
        );

        return materialDelayRepository.save(materialDelay);
    }

    public List<MaterialDelay> getAllMaterialDelays() {
        return materialDelayRepository.findAll();
    }

    public List<MaterialDelay> getMaterialDelaysByOrderId(Long orderId) {
        return materialDelayRepository.findByOrderId(orderId);
    }

    public void deleteMaterialDelay(Long id) {
        if (!materialDelayRepository.existsById(id)) {
            throw new ResourceNotFoundException("Material delay not found: " + id);
        }
        materialDelayRepository.deleteById(id);
    }
}
