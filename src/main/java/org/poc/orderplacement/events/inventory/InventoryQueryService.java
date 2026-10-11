package org.poc.orderplacement.events.inventory;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Narrow, read-only query surface other features use to check inventory progress for an order. */
@Service
public class InventoryQueryService {

    private final InventoryReservationRepository repository;

    public InventoryQueryService(final InventoryReservationRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public boolean isReserved(final UUID orderId) {
        return repository.existsByOrderId(orderId);
    }
}
