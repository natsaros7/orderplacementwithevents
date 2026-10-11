package org.poc.orderplacement.events.inventory;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Narrow, read-only query surface the {@code order} feature uses to build its status view. */
@Service
public class InventoryQueryService {

    private final InventoryReservationRepository repository;

    public InventoryQueryService(final InventoryReservationRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Optional<Instant> findReservedAt(final UUID orderId) {
        return repository.findByOrderId(orderId).map(InventoryReservation::getReservedAt);
    }
}
