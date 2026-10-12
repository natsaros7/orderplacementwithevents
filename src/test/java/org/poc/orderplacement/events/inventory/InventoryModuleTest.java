package org.poc.orderplacement.events.inventory;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.poc.orderplacement.events.TestcontainersConfiguration;
import org.poc.orderplacement.events.order.OrderPlaced;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.Scenario;

import static org.assertj.core.api.Assertions.assertThat;

/** Module-level test for {@code inventory}: publishing {@code order}'s event reserves stock. */
@ApplicationModuleTest
@Import(TestcontainersConfiguration.class)
class InventoryModuleTest {

    @Autowired
    private InventoryReservationRepository repository;

    @Test
    void should_reserveInventory_when_orderPlacedEventPublished(final Scenario scenario) {
        final OrderPlaced event = new OrderPlaced(UUID.randomUUID(), "SKU-MODULE", 3, Instant.now());

        scenario.publish(event)
                .andWaitForStateChange(() -> repository.existsByOrderId(event.orderId()))
                .andVerify(exists -> assertThat(exists).isTrue());
    }
}
