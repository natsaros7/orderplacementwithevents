package org.poc.orderplacement.events.shipping;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Narrow, read-only query surface other features use to check shipping progress for an order. */
@Service
public class ShippingQueryService {

    private final ShipmentRepository repository;

    public ShippingQueryService(final ShipmentRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public boolean isPrepared(final UUID orderId) {
        return repository.existsByOrderId(orderId);
    }
}
