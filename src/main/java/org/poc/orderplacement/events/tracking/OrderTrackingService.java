package org.poc.orderplacement.events.tracking;

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

    private final OrderQueryService orderQueryService;
    private final InventoryQueryService inventoryQueryService;
    private final ShippingQueryService shippingQueryService;

    public OrderTrackingService(final OrderQueryService orderQueryService,
            final InventoryQueryService inventoryQueryService, final ShippingQueryService shippingQueryService) {
        this.orderQueryService = orderQueryService;
        this.inventoryQueryService = inventoryQueryService;
        this.shippingQueryService = shippingQueryService;
    }

    public Optional<OrderTrackingView> findTracking(final UUID orderId) {
        return orderQueryService.findSummary(orderId).map(this::toTrackingView);
    }

    private OrderTrackingView toTrackingView(final OrderSummary summary) {
        return new OrderTrackingView(
                summary.orderId(),
                summary.productCode(),
                summary.quantity(),
                summary.placedAt(),
                inventoryQueryService.isReserved(summary.orderId()),
                shippingQueryService.isPrepared(summary.orderId()));
    }
}
