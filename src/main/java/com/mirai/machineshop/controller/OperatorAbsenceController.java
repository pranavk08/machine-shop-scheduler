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
import com.mirai.machineshop.dto.OperatorAbsenceRequest;
import com.mirai.machineshop.entity.OperatorAbsence;
import com.mirai.machineshop.service.OperatorAbsenceService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

@RestController
@RequestMapping("/api/operator-absences")
@Validated
public class OperatorAbsenceController {

    private final OperatorAbsenceService operatorAbsenceService;

    public OperatorAbsenceController(OperatorAbsenceService operatorAbsenceService) {
        this.operatorAbsenceService = operatorAbsenceService;
    }

    @GetMapping
    public List<OperatorAbsence> getAllAbsences() {
        return operatorAbsenceService.getAllAbsences();
    }

    @GetMapping("/operator/{operatorId}")
    public List<OperatorAbsence> getAbsencesByOperator(@Positive @PathVariable Long operatorId) {
        return operatorAbsenceService.getAbsencesByOperatorId(operatorId);
    }

    @PostMapping
    public ResponseEntity<OperatorAbsence> createAbsence(@Valid @RequestBody OperatorAbsenceRequest request) {
        OperatorAbsence created = operatorAbsenceService.createAbsence(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAbsence(@Positive @PathVariable Long id) {
        operatorAbsenceService.deleteAbsence(id);
        return ResponseEntity.noContent().build();
    }
}