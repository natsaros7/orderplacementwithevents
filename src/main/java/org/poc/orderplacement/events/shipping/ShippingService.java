package org.poc.orderplacement.events.shipping;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShippingService {

    private static final Logger log = LoggerFactory.getLogger(ShippingService.class);

    private final ShipmentRepository repository;

    public ShippingService(final ShipmentRepository repository) {
        this.repository = repository;
    }

    /**
     * Runs in its own transaction (REQUIRES_NEW) so a failure here never rolls back the order that
     * already committed. Idempotent on {@code orderId} via the exists-check plus the unique
     * constraint on {@code order_id}: concurrent redelivery can still race past the exists-check,
     * so {@code saveAndFlush} forces the constraint violation to surface here, inside the method,
     * rather than later at commit time where the caller could no longer handle it. The caller
     * (the listener) is expected to treat {@link org.springframework.dao.DataIntegrityViolationException}
     * as "someone else already prepared it", not as a failure.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void prepare(final UUID orderId) {
        if (repository.existsByOrderId(orderId)) {
            log.info("Shipment already prepared for order {}, skipping", orderId);
            return;
        }
        repository.saveAndFlush(Shipment.prepare(orderId));
        log.info("Prepared shipment for order {}", orderId);
    }
}
