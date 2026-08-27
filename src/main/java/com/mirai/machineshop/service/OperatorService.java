package com.mirai.machineshop.service;

import com.mirai.machineshop.entity.Operator;
import com.mirai.machineshop.repository.OperatorRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OperatorService {

    private final OperatorRepository operatorRepository;

    public OperatorService(OperatorRepository operatorRepository) {
        this.operatorRepository = operatorRepository;
    }

    public Operator createOperator(Operator operator) {
        return operatorRepository.save(operator);
    }

    public List<Operator> getAllOperators() {
        return operatorRepository.findAll();
    }

    public Operator getOperatorById(Long id) {
        return operatorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Operator not found"));
    }

    public void deleteOperator(Long id) {
        operatorRepository.deleteById(id);
    }
}