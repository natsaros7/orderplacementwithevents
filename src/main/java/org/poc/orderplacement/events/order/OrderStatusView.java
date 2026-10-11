package org.poc.orderplacement.events.order;

import java.time.Instant;
import java.util.UUID;

/** Read model assembled from the order itself plus inventory/shipping's independent progress. */
public record OrderStatusView(
        UUID orderId,
        String productCode,
        int quantity,
        Instant placedAt,
        boolean inventoryReserved,
        boolean shipmentPrepared) {
}
