package com.mirai.machineshop.entity;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "operator_shifts")
public class OperatorShift {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "operator_id", nullable = false)
    private Operator operator;

    @ManyToOne
    @JoinColumn(name = "shift_id", nullable = false)
    private Shift shift;

    @Column(nullable = false)
    private LocalDate workDate;

    @Column(nullable = false)
    private boolean available = true;

    public OperatorShift() {
    }

    public OperatorShift(Operator operator,
                         Shift shift,
                         LocalDate workDate,
                         boolean available) {
        this.operator = operator;
        this.shift = shift;
        this.workDate = workDate;
        this.available = available;
    }

    public Long getId() {
        return id;
    }

    public Operator getOperator() {
        return operator;
    }

    public void setOperator(Operator operator) {
        this.operator = operator;
    }

    public Shift getShift() {
        return shift;
    }

    public void setShift(Shift shift) {
        this.shift = shift;
    }

    public LocalDate getWorkDate() {
        return workDate;
    }

    public void setWorkDate(LocalDate workDate) {
        this.workDate = workDate;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }
}