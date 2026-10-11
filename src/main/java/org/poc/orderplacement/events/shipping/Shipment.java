package org.poc.orderplacement.events.shipping;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "shipments",
        uniqueConstraints = @UniqueConstraint(name = "uk_shipments_order_id", columnNames = "order_id"))
public class Shipment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    private Instant preparedAt;

    protected Shipment() {
        // JPA
    }

    private Shipment(final UUID orderId, final Instant preparedAt) {
        this.orderId = orderId;
        this.preparedAt = preparedAt;
    }

    public static Shipment prepare(final UUID orderId) {
        return new Shipment(orderId, Instant.now());
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public Instant getPreparedAt() {
        return preparedAt;
    }
}
