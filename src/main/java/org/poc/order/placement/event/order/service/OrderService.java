package org.poc.order.placement.event.order.service;

import org.poc.order.placement.event.order.entities.Order;
import org.poc.order.placement.event.order.event.OrderPlacedEvent;
import org.poc.order.placement.event.order.repositories.OrderRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class OrderService {
    private final ApplicationEventPublisher eventPublisher;
    private final OrderRepository orderRepository;

    public OrderService(ApplicationEventPublisher eventPublisher, OrderRepository orderRepository) {
        this.eventPublisher = eventPublisher;
        this.orderRepository = orderRepository;
    }

    public Order placeOrder(Order order) {
        // Save order
        order = orderRepository.save(order);
        // Publish an event
        eventPublisher.publishEvent(new OrderPlacedEvent(this, order));
        return order;
    }
}
