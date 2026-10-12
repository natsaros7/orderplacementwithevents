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
     * Deliberately still {@code @Transactional(REQUIRES_NEW)} here, even though the caller
     * ({@link ShippingEventListener}'s {@code @ApplicationModuleListener}) already opens its own
     * REQUIRES_NEW transaction: {@code saveAndFlush} throwing marks whichever transaction is
     * currently active as rollback-only, and {@link ShippingEventListener} catches that exception
     * <em>inside</em> its own transactional method. Without a nested REQUIRES_NEW here, the caught
     * exception would leave the listener's transaction poisoned — it would still try to commit on
     * normal return and fail with {@code UnexpectedRollbackException} instead. With it, the
     * exception-and-rollback happens cleanly in this inner transaction, and the listener's own
     * (now untouched) transaction commits normally.
     * <p>
     * Idempotent on {@code orderId} via the exists-check plus the unique constraint on
     * {@code order_id}: concurrent redelivery can still race past the exists-check, so
     * {@code saveAndFlush} forces the constraint violation to surface here, inside the method,
     * rather than later at commit time where the caller could no longer handle it. The caller is
     * expected to treat {@link org.springframework.dao.DataIntegrityViolationException} as "someone
     * else already prepared it", not as a failure.
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
