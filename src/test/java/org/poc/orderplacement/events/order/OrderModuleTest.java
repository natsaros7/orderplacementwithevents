package org.poc.orderplacement.events.order;

import org.junit.jupiter.api.Test;
import org.poc.orderplacement.events.TestcontainersConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.Scenario;

import static org.assertj.core.api.Assertions.assertThat;

/** Module-level test for {@code order}, bootstrapped standalone (its only dependency is none). */
@ApplicationModuleTest
@Import(TestcontainersConfiguration.class)
class OrderModuleTest {

    @Autowired
    private OrderService orderService;

    @Test
    void should_publishOrderPlaced_when_orderIsPlaced(final Scenario scenario) {
        final PlaceOrderCommand command = new PlaceOrderCommand("SKU-MODULE", 2);

        scenario.stimulate(() -> orderService.placeOrder(command))
                .andWaitForEventOfType(OrderPlaced.class)
                .toArriveAndVerify(event -> {
                    assertThat(event.productCode()).isEqualTo("SKU-MODULE");
                    assertThat(event.quantity()).isEqualTo(2);
                });
    }
}
