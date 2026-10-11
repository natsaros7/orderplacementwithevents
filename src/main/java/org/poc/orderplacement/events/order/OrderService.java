package org.poc.orderplacement.events.order;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final ApplicationEventPublisher eventPublisher;

    public OrderService(final OrderRepository orderRepository, final ApplicationEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public UUID placeOrder(final PlaceOrderCommand command) {
        final Order saved = orderRepository.save(Order.place(command.productCode(), command.quantity()));
        eventPublisher.publishEvent(
                new OrderPlaced(saved.getId(), saved.getProductCode(), saved.getQuantity(), saved.getPlacedAt()));
        log.info("Placed order {} for product {} (quantity {})", saved.getId(), saved.getProductCode(),
                saved.getQuantity());
        return saved.getId();
    }
}
