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
     * Runs in its own transaction (REQUIRES_NEW) so a failure here never rolls back the order that
     * already committed. Idempotent on {@code orderId} so redelivery or retry can't double-reserve.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void reserve(final UUID orderId, final String productCode, final int quantity) {
        if (repository.existsByOrderId(orderId)) {
            log.info("Inventory already reserved for order {}, skipping", orderId);
            return;
        }
        repository.save(InventoryReservation.reserve(orderId, productCode, quantity));
        log.info("Reserved {} unit(s) of {} for order {}", quantity, productCode, orderId);
    }
}
