package com.mirai.machineshop.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "operations")
public class Operation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(nullable = false)
    private Integer sequenceNumber;

    @Column(nullable = false)
    private String operationType;

    @Column(nullable = false)
    private Integer processingTimeMinutes;

    @Column(nullable = false)
    private String requiredMachineType;

    public Operation() {
    }

    public Operation(Order order,
                     Integer sequenceNumber,
                     String operationType,
                     Integer processingTimeMinutes,
                     String requiredMachineType) {
        this.order = order;
        this.sequenceNumber = sequenceNumber;
        this.operationType = operationType;
        this.processingTimeMinutes = processingTimeMinutes;
        this.requiredMachineType = requiredMachineType;
    }

    public Long getId() {
        return id;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public Integer getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(Integer sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    public Integer getProcessingTimeMinutes() {
        return processingTimeMinutes;
    }

    public void setProcessingTimeMinutes(Integer processingTimeMinutes) {
        this.processingTimeMinutes = processingTimeMinutes;
    }

    public String getRequiredMachineType() {
        return requiredMachineType;
    }

    public void setRequiredMachineType(String requiredMachineType) {
        this.requiredMachineType = requiredMachineType;
    }
}