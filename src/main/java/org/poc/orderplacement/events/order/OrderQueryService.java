package org.poc.orderplacement.events.order;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Narrow, read-only query surface other features use to look up an order. {@code order} knows
 * nothing about inventory or shipping — it only exposes its own data.
 */
@Service
public class OrderQueryService {

    private final OrderRepository orderRepository;

    public OrderQueryService(final OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public Optional<OrderSummary> findSummary(final UUID orderId) {
        return orderRepository.findById(orderId)
                .map(order -> new OrderSummary(order.getId(), order.getProductCode(), order.getQuantity(),
                        order.getPlacedAt()));
    }
}
