package org.poc.orderplacement.events.inventory;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class InventoryReservationRepositoryTest {

    @Autowired
    private InventoryReservationRepository repository;

    @Test
    void should_findByOrderId_when_reservationExists() {
        final UUID orderId = UUID.randomUUID();
        repository.saveAndFlush(InventoryReservation.reserve(orderId, "SKU-1", 2));

        assertThat(repository.findByOrderId(orderId)).isPresent();
        assertThat(repository.existsByOrderId(orderId)).isTrue();
    }

    @Test
    void should_rejectSecondReservation_when_orderIdAlreadyReserved() {
        final UUID orderId = UUID.randomUUID();
        repository.saveAndFlush(InventoryReservation.reserve(orderId, "SKU-1", 2));

        assertThatThrownBy(() -> repository.saveAndFlush(InventoryReservation.reserve(orderId, "SKU-2", 1)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
