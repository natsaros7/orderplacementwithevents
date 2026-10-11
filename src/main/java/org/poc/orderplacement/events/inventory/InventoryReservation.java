package org.poc.orderplacement.events.inventory;

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
@Table(name = "inventory_reservations", uniqueConstraints = @UniqueConstraint(columnNames = "order_id"))
public class InventoryReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_id", nullable = false, unique = true)
    private UUID orderId;

    private String productCode;

    private int quantity;

    private Instant reservedAt;

    protected InventoryReservation() {
        // JPA
    }

    private InventoryReservation(final UUID orderId, final String productCode, final int quantity,
            final Instant reservedAt) {
        this.orderId = orderId;
        this.productCode = productCode;
        this.quantity = quantity;
        this.reservedAt = reservedAt;
    }

    public static InventoryReservation reserve(final UUID orderId, final String productCode, final int quantity) {
        return new InventoryReservation(orderId, productCode, quantity, Instant.now());
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public String getProductCode() {
        return productCode;
    }

    public int getQuantity() {
        return quantity;
    }

    public Instant getReservedAt() {
        return reservedAt;
    }
}
