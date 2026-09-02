package com.mirai.machineshop.controller;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mirai.machineshop.dto.MaterialDelayRequest;
import com.mirai.machineshop.entity.MaterialDelay;
import com.mirai.machineshop.service.MaterialDelayService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

@RestController
@RequestMapping("/api/material-delays")
@Validated
public class MaterialDelayController {

    private final MaterialDelayService materialDelayService;

    public MaterialDelayController(MaterialDelayService materialDelayService) {
        this.materialDelayService = materialDelayService;
    }

    @GetMapping
    public List<MaterialDelay> getAllMaterialDelays() {
        return materialDelayService.getAllMaterialDelays();
    }

    @GetMapping("/order/{orderId}")
    public List<MaterialDelay> getMaterialDelaysByOrder(@Positive @PathVariable Long orderId) {
        return materialDelayService.getMaterialDelaysByOrderId(orderId);
    }

    @PostMapping
    public ResponseEntity<MaterialDelay> createMaterialDelay(@Valid @RequestBody MaterialDelayRequest request) {
        MaterialDelay created = materialDelayService.createMaterialDelay(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMaterialDelay(@Positive @PathVariable Long id) {
        materialDelayService.deleteMaterialDelay(id);
        return ResponseEntity.noContent().build();
    }
}
