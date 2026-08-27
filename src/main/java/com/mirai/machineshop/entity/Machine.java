package com.mirai.machineshop.entity;

import jakarta.persistence.*;

@Entity   
@Table(name = "machines")
public class Machine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String machineCode;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String type;

    private boolean available = true;

    public Machine() {
    }

    public Machine(String machineCode, String name, String type) {
        this.machineCode = machineCode;
        this.name = name;
        this.type = type;
    }

    public Long getId() {
        return id;
    }

    public String getMachineCode() {
        return machineCode;
    }

    public void setMachineCode(String machineCode) {
        this.machineCode = machineCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }
}
