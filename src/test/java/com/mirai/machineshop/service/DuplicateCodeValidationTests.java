package com.mirai.machineshop.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.mirai.machineshop.dto.MachineRequest;
import com.mirai.machineshop.dto.OperatorRequest;
import com.mirai.machineshop.exception.DuplicateResourceException;
import com.mirai.machineshop.repository.MachineRepository;
import com.mirai.machineshop.repository.OperatorRepository;

class DuplicateCodeValidationTests {

    @Test
    void rejectsDuplicateMachineCodeBeforeSaving() {
        MachineRepository repository = mock(MachineRepository.class);
        when(repository.existsByMachineCodeIgnoreCase("M-001")).thenReturn(true);

        MachineService service = new MachineService(repository);

        assertThrows(
                DuplicateResourceException.class,
                () -> service.createMachine(
                        new MachineRequest("M-001", "Lathe", "TURNING", true)));
    }

    @Test
    void rejectsDuplicateOperatorCodeBeforeSaving() {
        OperatorRepository repository = mock(OperatorRepository.class);
        when(repository.existsByOperatorCodeIgnoreCase("OP-001")).thenReturn(true);

        OperatorService service = new OperatorService(repository);

        assertThrows(
                DuplicateResourceException.class,
                () -> service.createOperator(
                        new OperatorRequest("OP-001", "Ravi", true)));
    }
}
