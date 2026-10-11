package org.poc.orderplacement.events.inventory;

import org.poc.orderplacement.events.order.OrderPlaced;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class InventoryEventListener {

    private static final Logger log = LoggerFactory.getLogger(InventoryEventListener.class);

    private final InventoryService inventoryService;

    public InventoryEventListener(final InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    /**
     * AFTER_COMMIT: only reserve stock once the order is durably committed. If the app crashes
     * between commit and this callback running, the reservation is lost — see README "Design notes".
     * <p>
     * The unique-constraint violation from a concurrently delivered duplicate is raised by
     * {@link InventoryService#reserve} itself (via {@code saveAndFlush}), not by the
     * {@code REQUIRES_NEW} proxy at commit time, so it is still visible here and can be treated as
     * a benign duplicate rather than a real failure.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderPlaced(final OrderPlaced event) {
        try {
            inventoryService.reserve(event.orderId(), event.productCode(), event.quantity());
        } catch (final DataIntegrityViolationException exception) {
            log.info("Reservation for order {} already created concurrently, skipping", event.orderId());
        }
    }
}
