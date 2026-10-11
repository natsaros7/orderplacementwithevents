package org.poc.orderplacement.events.shipping;

import org.poc.orderplacement.events.order.OrderPlaced;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class ShippingEventListener {

    private static final Logger log = LoggerFactory.getLogger(ShippingEventListener.class);

    private final ShippingService shippingService;

    public ShippingEventListener(final ShippingService shippingService) {
        this.shippingService = shippingService;
    }

    /**
     * AFTER_COMMIT: only prepare shipping once the order is durably committed. If the app crashes
     * between commit and this callback running, the shipment is lost — see README "Design notes".
     * <p>
     * The unique-constraint violation from a concurrently delivered duplicate is raised by
     * {@link ShippingService#prepare} itself (via {@code saveAndFlush}), not by the
     * {@code REQUIRES_NEW} proxy at commit time, so it is still visible here and can be treated as
     * a benign duplicate rather than a real failure.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderPlaced(final OrderPlaced event) {
        try {
            shippingService.prepare(event.orderId());
        } catch (final DataIntegrityViolationException exception) {
            log.info("Shipment for order {} already prepared concurrently, skipping", event.orderId());
        }
    }
}
