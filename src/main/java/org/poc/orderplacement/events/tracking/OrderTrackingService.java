package org.poc.orderplacement.events.tracking;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.poc.orderplacement.events.inventory.InventoryQueryService;
import org.poc.orderplacement.events.order.OrderQueryService;
import org.poc.orderplacement.events.order.OrderSummary;
import org.poc.orderplacement.events.shipping.ShippingQueryService;
import org.springframework.stereotype.Service;

/**
 * Assembles the cross-feature read model from order's, inventory's and shipping's public query
 * services only — never their entities or repositories directly. This is the only feature allowed
 * to depend on all three; {@code order} itself knows nothing about inventory or shipping.
 */
@Service
public class OrderTrackingService {

    /** How long a pending order is still considered "settling" before the UI gives up polling. */
    static final Duration SETTLING_WINDOW = Duration.ofSeconds(30);

    private final OrderQueryService orderQueryService;
    private final InventoryQueryService inventoryQueryService;
    private final ShippingQueryService shippingQueryService;
    private final Clock clock;

    public OrderTrackingService(final OrderQueryService orderQueryService,
            final InventoryQueryService inventoryQueryService, final ShippingQueryService shippingQueryService,
            final Clock clock) {
        this.orderQueryService = orderQueryService;
        this.inventoryQueryService = inventoryQueryService;
        this.shippingQueryService = shippingQueryService;
        this.clock = clock;
    }

    public Optional<OrderTrackingView> findTracking(final UUID orderId) {
        return orderQueryService.findSummary(orderId).map(this::toTrackingView);
    }

    private OrderTrackingView toTrackingView(final OrderSummary summary) {
        final boolean inventoryReserved = inventoryQueryService.isReserved(summary.orderId());
        final boolean shipmentPrepared = shippingQueryService.isPrepared(summary.orderId());
        final boolean pending = !inventoryReserved || !shipmentPrepared;
        final boolean settling = pending && isWithinSettlingWindow(summary.placedAt());
        return new OrderTrackingView(
                summary.orderId(),
                summary.productCode(),
                summary.quantity(),
                summary.placedAt(),
                inventoryReserved,
                shipmentPrepared,
                settling);
    }

    private boolean isWithinSettlingWindow(final Instant placedAt) {
        return Duration.between(placedAt, Instant.now(clock)).compareTo(SETTLING_WINDOW) < 0;
    }
}
