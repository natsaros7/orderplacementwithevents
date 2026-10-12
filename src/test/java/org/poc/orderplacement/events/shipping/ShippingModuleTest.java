package org.poc.orderplacement.events.shipping;

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

/** Module-level test for {@code shipping}: publishing {@code order}'s event prepares a shipment. */
@ApplicationModuleTest
@Import(TestcontainersConfiguration.class)
class ShippingModuleTest {

    @Autowired
    private ShipmentRepository repository;

    @Test
    void should_prepareShipment_when_orderPlacedEventPublished(final Scenario scenario) {
        final OrderPlaced event = new OrderPlaced(UUID.randomUUID(), "SKU-MODULE", 1, Instant.now());

        scenario.publish(event)
                .andWaitForStateChange(() -> repository.existsByOrderId(event.orderId()))
                .andVerify(exists -> assertThat(exists).isTrue());
    }
}
