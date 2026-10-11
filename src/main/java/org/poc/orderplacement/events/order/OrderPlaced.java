package org.poc.orderplacement.events.order;

import java.time.Instant;
import java.util.UUID;

/**
 * Published after an order is committed. Carries only the order id and the facts listeners need —
 * never the {@link Order} entity itself — so listeners can't accidentally mutate or lazily load
 * persistence state outside its transaction.
 */
public record OrderPlaced(UUID orderId, String productCode, int quantity, Instant placedAt) {
}
