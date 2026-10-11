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
     * already committed. Idempotent on {@code orderId} so redelivery or retry can't double-prepare.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void prepare(final UUID orderId) {
        if (repository.existsByOrderId(orderId)) {
            log.info("Shipment already prepared for order {}, skipping", orderId);
            return;
        }
        repository.save(Shipment.prepare(orderId));
        log.info("Prepared shipment for order {}", orderId);
    }
}
