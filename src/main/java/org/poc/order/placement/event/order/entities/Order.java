package org.poc.order.placement.event.order.entities;

import jakarta.persistence.*;

import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(name = "orders") // Changed from "order" to avoid SQL keyword conflict
public class Order implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    private String status;

    public Order() {
    }

    public Order(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public UUID getId() {
        return id;
    }
}
