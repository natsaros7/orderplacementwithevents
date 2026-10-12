package org.poc.orderplacement.events.inventory;

import org.poc.orderplacement.events.order.OrderPlaced;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
public class InventoryEventListener {

    private static final Logger log = LoggerFactory.getLogger(InventoryEventListener.class);

    private final InventoryService inventoryService;

    public InventoryEventListener(final InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    /**
     * {@code @ApplicationModuleListener} composes {@code @Async}, {@code @TransactionalEventListener}
     * (AFTER_COMMIT by default — only reserve stock once the order is durably committed) and
     * {@code @Transactional(propagation = REQUIRES_NEW)}, and registers this invocation with
     * Modulith's event publication registry: if this method throws, the publication stays
     * incomplete (durably, in the {@code event_publication} table) instead of just being lost, and
     * gets resubmitted — closing the AFTER_COMMIT-event-lost-on-crash gap from the previous design.
     * <p>
     * The unique-constraint violation from a concurrently delivered duplicate is raised by
     * {@link InventoryService#reserve} itself (via {@code saveAndFlush}), not at commit time, so
     * it is still visible here and can be treated as a benign duplicate rather than a real failure
     * (and is NOT what leaves a publication incomplete — only an uncaught exception does that).
     */
    @ApplicationModuleListener
    public void onOrderPlaced(final OrderPlaced event) {
        try {
            inventoryService.reserve(event.orderId(), event.productCode(), event.quantity());
        } catch (final DataIntegrityViolationException exception) {
            log.info("Reservation for order {} already created concurrently, skipping", event.orderId());
        }
    }
}
