package org.poc.order.placement.event.inventory;

import org.poc.order.placement.event.order.event.OrderPlacedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class InventoryService {

    @EventListener
    public void handleOrderPlacedEvent(OrderPlacedEvent event) {
        // Logic to update inventory
        System.out.println("Updating inventory for order " + event.getOrder().getId());
    }
}
