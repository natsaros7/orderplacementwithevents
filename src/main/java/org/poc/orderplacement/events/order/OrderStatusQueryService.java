package org.poc.orderplacement.events.order;

import java.util.Optional;
import java.util.UUID;

import org.poc.orderplacement.events.inventory.InventoryQueryService;
import org.poc.orderplacement.events.shipping.ShippingQueryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Assembles the read model from the order plus inventory/shipping's public query services only —
 * never their entities or repositories directly.
 */
@Service
public class OrderStatusQueryService {

    private final OrderRepository orderRepository;
    private final InventoryQueryService inventoryQueryService;
    private final ShippingQueryService shippingQueryService;

    public OrderStatusQueryService(final OrderRepository orderRepository,
            final InventoryQueryService inventoryQueryService, final ShippingQueryService shippingQueryService) {
        this.orderRepository = orderRepository;
        this.inventoryQueryService = inventoryQueryService;
        this.shippingQueryService = shippingQueryService;
    }

    @Transactional(readOnly = true)
    public Optional<OrderStatusView> findStatus(final UUID orderId) {
        return orderRepository.findById(orderId).map(order -> new OrderStatusView(
                order.getId(),
                order.getProductCode(),
                order.getQuantity(),
                order.getPlacedAt(),
                inventoryQueryService.findReservedAt(orderId).isPresent(),
                shippingQueryService.findPreparedAt(orderId).isPresent()));
    }
}
