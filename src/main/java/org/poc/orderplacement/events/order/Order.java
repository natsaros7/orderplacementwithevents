package org.poc.orderplacement.events.order;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    private UUID id;

    @Column(length = 64)
    private String productCode;

    private int quantity;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private Instant placedAt;

    protected Order() {
        // JPA
    }

    private Order(final UUID id, final String productCode, final int quantity, final Instant placedAt) {
        this.id = id;
        this.productCode = productCode;
        this.quantity = quantity;
        this.placedAt = placedAt;
        this.status = OrderStatus.PLACED;
    }

    public static Order place(final String productCode, final int quantity) {
        return new Order(UUID.randomUUID(), productCode, quantity, Instant.now());
    }

    public UUID getId() {
        return id;
    }

    public String getProductCode() {
        return productCode;
    }

    public int getQuantity() {
        return quantity;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public Instant getPlacedAt() {
        return placedAt;
    }
}
