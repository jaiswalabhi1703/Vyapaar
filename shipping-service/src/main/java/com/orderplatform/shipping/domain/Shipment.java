package com.orderplatform.shipping.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "shipment")
public class Shipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false, unique = true)
    private Long orderId;

    @Column(nullable = false)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShipmentStatus status;

    protected Shipment() {
    }

    public Shipment(Long orderId, String address) {
        this.orderId = orderId;
        this.address = address;
        this.status = ShipmentStatus.SCHEDULED;
    }

    public void cancel() {
        this.status = ShipmentStatus.CANCELLED;
    }

    public Long getId() {
        return id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public String getAddress() {
        return address;
    }

    public ShipmentStatus getStatus() {
        return status;
    }
}
