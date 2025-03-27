package org.poc.order.placement.event.shiping;

import org.poc.order.placement.event.order.event.OrderPlacedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ShippingService {

    @EventListener
    public void handleOrderPlacedEvent(OrderPlacedEvent event) {
        // Logic to prepare the order for shipping
        System.out.println("Preparing shipping for order " + event.getOrder().getId());
    }
}
