package com.mirai.machineshop.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String orderNumber;

    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private LocalDateTime dueDate;

    @Column(nullable = false)
    private String status;
    
    @Column(nullable = false)
    private String partFamily;

    public Order() {
    }

    public Order(String orderNumber, Customer customer,
            Integer quantity, String partFamily,
            LocalDateTime dueDate, String status) {
        this.orderNumber = orderNumber;
        this.customer = customer;
        this.quantity = quantity;
        this.partFamily = partFamily;
        this.dueDate = dueDate;
        this.status = status;
    }
    
    public String getPartFamily() {
        return partFamily;
    }

    public void setPartFamily(String partFamily) {
        this.partFamily = partFamily;
    }

    public Long getId() {
        return id;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public LocalDateTime getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDateTime dueDate) {
        this.dueDate = dueDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}