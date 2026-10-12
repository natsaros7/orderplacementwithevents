package org.poc.orderplacement.events.tracking;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.poc.orderplacement.events.TestcontainersConfiguration;
import org.poc.orderplacement.events.order.OrderService;
import org.poc.orderplacement.events.order.PlaceOrderCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.ApplicationModuleTest.BootstrapMode;
import org.springframework.modulith.test.Scenario;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Module-level test for {@code tracking}, the only module allowed to depend on order, inventory
 * and shipping at once — bootstrapped with its direct dependencies so OrderService and the two
 * listeners are real, not mocked.
 */
@ApplicationModuleTest(mode = BootstrapMode.DIRECT_DEPENDENCIES)
@Import(TestcontainersConfiguration.class)
class TrackingModuleTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderTrackingService orderTrackingService;

    @Test
    void should_reportInventoryAndShipmentReserved_when_orderPlacedAndListenersComplete(final Scenario scenario) {
        final AtomicReference<UUID> placedOrderId = new AtomicReference<>();

        scenario.stimulate(() -> placedOrderId.set(orderService.placeOrder(new PlaceOrderCommand("SKU-MODULE", 1))))
                .andWaitForStateChange(() -> orderTrackingService.findTracking(placedOrderId.get())
                        .map(view -> view.inventoryReserved() && view.shipmentPrepared())
                        .orElse(false))
                .andVerify(ready -> assertThat(ready).isTrue());
    }
}
