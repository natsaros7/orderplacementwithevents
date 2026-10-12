package org.poc.orderplacement.events.inventory;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.poc.orderplacement.events.TestcontainersConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import(TestcontainersConfiguration.class)
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
