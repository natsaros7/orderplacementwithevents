package org.poc.orderplacement.events.tracking;

import java.time.Instant;
import java.util.UUID;

/** Read model assembled from the order plus inventory/shipping's independent progress. */
public record OrderTrackingView(
        UUID orderId,
        String productCode,
        int quantity,
        Instant placedAt,
        boolean inventoryReserved,
        boolean shipmentPrepared) {
}
