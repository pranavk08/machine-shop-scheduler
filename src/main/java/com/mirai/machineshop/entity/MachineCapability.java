package com.mirai.machineshop.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "machine_capabilities")
public class MachineCapability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "machine_id", nullable = false)
    private Machine machine;

    @Column(nullable = false)
    private String capability;

    public MachineCapability() {
    }

    public MachineCapability(Machine machine, String capability) {
        this.machine = machine;
        this.capability = capability;
    }

    public Long getId() {
        return id;
    }

    public Machine getMachine() {
        return machine;
    }

    public void setMachine(Machine machine) {
        this.machine = machine;
    }

    public String getCapability() {
        return capability;
    }

    public void setCapability(String capability) {
        this.capability = capability;
    }
}