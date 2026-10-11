package org.poc.orderplacement.events.shipping;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class ShipmentRepositoryTest {

    @Autowired
    private ShipmentRepository repository;

    @Test
    void should_findByOrderId_when_shipmentExists() {
        final UUID orderId = UUID.randomUUID();
        repository.saveAndFlush(Shipment.prepare(orderId));

        assertThat(repository.findByOrderId(orderId)).isPresent();
        assertThat(repository.existsByOrderId(orderId)).isTrue();
    }

    @Test
    void should_rejectSecondShipment_when_orderIdAlreadyPrepared() {
        final UUID orderId = UUID.randomUUID();
        repository.saveAndFlush(Shipment.prepare(orderId));

        assertThatThrownBy(() -> repository.saveAndFlush(Shipment.prepare(orderId)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
