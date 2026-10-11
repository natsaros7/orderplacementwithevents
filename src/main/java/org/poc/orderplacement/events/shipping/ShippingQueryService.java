package org.poc.orderplacement.events.shipping;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Narrow, read-only query surface the {@code order} feature uses to build its status view. */
@Service
public class ShippingQueryService {

    private final ShipmentRepository repository;

    public ShippingQueryService(final ShipmentRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Optional<Instant> findPreparedAt(final UUID orderId) {
        return repository.findByOrderId(orderId).map(Shipment::getPreparedAt);
    }
}
