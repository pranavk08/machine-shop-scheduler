package com.mirai.machineshop.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "operators")
public class Operator {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String operatorCode;

    @Column(nullable = false)
    private String name;

    private boolean available = true;

    public Operator() {
    }

    public Operator(String operatorCode, String name) {
        this.operatorCode = operatorCode;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getOperatorCode() {
        return operatorCode;
    }

    public void setOperatorCode(String operatorCode) {
        this.operatorCode = operatorCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }
}