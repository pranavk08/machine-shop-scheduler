package com.mirai.machineshop.service;

import com.mirai.machineshop.dto.OperatorRequest;
import com.mirai.machineshop.entity.Operator;
import com.mirai.machineshop.exception.DuplicateResourceException;
import com.mirai.machineshop.exception.ResourceNotFoundException;
import com.mirai.machineshop.repository.OperatorRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OperatorService {

    private final OperatorRepository operatorRepository;

    public OperatorService(OperatorRepository operatorRepository) {
        this.operatorRepository = operatorRepository;
    }

    public Operator createOperator(OperatorRequest request) {

        if (operatorRepository.existsByOperatorCodeIgnoreCase(request.operatorCode())) {
            throw new DuplicateResourceException(
                    "Operator code already exists: " + request.operatorCode());
        }

        Operator operator = new Operator(
                request.operatorCode().trim(),
                request.name().trim());

        if (request.available() != null) {
            operator.setAvailable(request.available());
        }

        return operatorRepository.save(operator);
    }

    public List<Operator> getAllOperators() {
        return operatorRepository.findAll();
    }

    public Operator getOperatorById(Long id) {
        return operatorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Operator not found: " + id));
    }

    public void deleteOperator(Long id) {
        if (!operatorRepository.existsById(id)) {
            throw new ResourceNotFoundException("Operator not found: " + id);
        }

        operatorRepository.deleteById(id);
    }
}
