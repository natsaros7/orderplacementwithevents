package org.poc.orderplacement.events.tracking;

import java.time.Instant;
import java.util.UUID;

/**
 * Read model assembled from the order plus inventory/shipping's independent progress.
 *
 * @param settling whether it's still reasonable to expect {@code inventoryReserved}/{@code
 *         shipmentPrepared} to flip to {@code true} soon; {@code false} once a pending order has
 *         been waiting long enough that a listener likely failed permanently (the scenario this
 *         POC exists to demonstrate), so the UI should stop polling and say so.
 */
public record OrderTrackingView(
        UUID orderId,
        String productCode,
        int quantity,
        Instant placedAt,
        boolean inventoryReserved,
        boolean shipmentPrepared,
        boolean settling) {
}
