package org.poc.orderplacement.events.inventory;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    private final InventoryReservationRepository repository;

    public InventoryService(final InventoryReservationRepository repository) {
        this.repository = repository;
    }

    /**
     * Deliberately still {@code @Transactional(REQUIRES_NEW)} here, even though the caller
     * ({@link InventoryEventListener}'s {@code @ApplicationModuleListener}) already opens its own
     * REQUIRES_NEW transaction: {@code saveAndFlush} throwing marks whichever transaction is
     * currently active as rollback-only, and {@link InventoryEventListener} catches that exception
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
     * else already reserved it", not as a failure.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void reserve(final UUID orderId, final String productCode, final int quantity) {
        if (repository.existsByOrderId(orderId)) {
            log.info("Inventory already reserved for order {}, skipping", orderId);
            return;
        }
        repository.saveAndFlush(InventoryReservation.reserve(orderId, productCode, quantity));
        log.info("Reserved {} unit(s) of {} for order {}", quantity, productCode, orderId);
    }
}
