package com.mirai.machineshop.service;

import java.util.List;
import org.springframework.stereotype.Service;
import com.mirai.machineshop.dto.OperatorAbsenceRequest;
import com.mirai.machineshop.entity.Operator;
import com.mirai.machineshop.entity.OperatorAbsence;
import com.mirai.machineshop.exception.InvalidBusinessRequestException;
import com.mirai.machineshop.exception.ResourceNotFoundException;
import com.mirai.machineshop.repository.OperatorAbsenceRepository;
import com.mirai.machineshop.repository.OperatorRepository;

@Service
public class OperatorAbsenceService {

    private final OperatorAbsenceRepository operatorAbsenceRepository;
    private final OperatorRepository operatorRepository;

    public OperatorAbsenceService(
            OperatorAbsenceRepository operatorAbsenceRepository,
            OperatorRepository operatorRepository) {
        this.operatorAbsenceRepository = operatorAbsenceRepository;
        this.operatorRepository = operatorRepository;
    }

    public OperatorAbsence createAbsence(OperatorAbsenceRequest request) {
        if (!request.endTime().isAfter(request.startTime())) {
            throw new InvalidBusinessRequestException("endTime must be after startTime.");
        }

        Operator operator = operatorRepository.findById(request.operatorId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Operator not found: " + request.operatorId()));

        OperatorAbsence absence = new OperatorAbsence(
                operator,
                request.startTime(),
                request.endTime(),
                request.reason().trim()
        );

        return operatorAbsenceRepository.save(absence);
    }

    public List<OperatorAbsence> getAllAbsences() {
        return operatorAbsenceRepository.findAll();
    }

    public List<OperatorAbsence> getAbsencesByOperatorId(Long operatorId) {
        return operatorAbsenceRepository.findByOperatorId(operatorId);
    }

    public void deleteAbsence(Long id) {
        if (!operatorAbsenceRepository.existsById(id)) {
            throw new ResourceNotFoundException("Operator absence not found: " + id);
        }
        operatorAbsenceRepository.deleteById(id);
    }
}