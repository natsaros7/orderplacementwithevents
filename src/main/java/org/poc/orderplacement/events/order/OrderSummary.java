package org.poc.orderplacement.events.order;

import java.time.Instant;
import java.util.UUID;

/** The narrow, public view of an order other features are allowed to see. */
public record OrderSummary(UUID orderId, String productCode, int quantity, Instant placedAt) {
}
