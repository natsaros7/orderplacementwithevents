package org.poc.orderplacement.events.inventory;

import org.poc.orderplacement.events.order.OrderPlaced;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class InventoryEventListener {

    private final InventoryService inventoryService;

    public InventoryEventListener(final InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    /**
     * AFTER_COMMIT: only reserve stock once the order is durably committed. If the app crashes
     * between commit and this callback running, the reservation is lost — see README "Design notes".
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderPlaced(final OrderPlaced event) {
        inventoryService.reserve(event.orderId(), event.productCode(), event.quantity());
    }
}
