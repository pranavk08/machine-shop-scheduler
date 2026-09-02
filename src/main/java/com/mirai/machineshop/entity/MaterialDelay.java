package com.mirai.machineshop.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "material_delays")
public class MaterialDelay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(nullable = false)
    private LocalDateTime delayedUntil;

    @Column(nullable = false)
    private String reason;

    public MaterialDelay() {
    }

    public MaterialDelay(
            Order order,
            LocalDateTime delayedUntil,
            String reason) {
        this.order = order;
        this.delayedUntil = delayedUntil;
        this.reason = reason;
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

    public LocalDateTime getDelayedUntil() {
        return delayedUntil;
    }

    public void setDelayedUntil(LocalDateTime delayedUntil) {
        this.delayedUntil = delayedUntil;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
