package com.mirai.machineshop.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "changeovers")
public class Changeover {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "machine_id", nullable = false)
    private Machine machine;

    @Column(nullable = false)
    private String fromPartFamily;

    @Column(nullable = false)
    private String toPartFamily;

    @Column(nullable = false)
    private Integer changeoverMinutes;

    public Changeover() {
    }

    public Changeover(Machine machine,
                      String fromPartFamily,
                      String toPartFamily,
                      Integer changeoverMinutes) {
        this.machine = machine;
        this.fromPartFamily = fromPartFamily;
        this.toPartFamily = toPartFamily;
        this.changeoverMinutes = changeoverMinutes;
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

    public String getFromPartFamily() {
        return fromPartFamily;
    }

    public void setFromPartFamily(String fromPartFamily) {
        this.fromPartFamily = fromPartFamily;
    }

    public String getToPartFamily() {
        return toPartFamily;
    }

    public void setToPartFamily(String toPartFamily) {
        this.toPartFamily = toPartFamily;
    }

    public Integer getChangeoverMinutes() {
        return changeoverMinutes;
    }

    public void setChangeoverMinutes(Integer changeoverMinutes) {
        this.changeoverMinutes = changeoverMinutes;
    }
}