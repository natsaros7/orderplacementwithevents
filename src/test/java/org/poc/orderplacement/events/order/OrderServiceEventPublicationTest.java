package org.poc.orderplacement.events.order;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import static org.assertj.core.api.Assertions.assertThat;

/** Proves OrderService actually publishes OrderPlaced, independent of what any listener does with it. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@RecordApplicationEvents
class OrderServiceEventPublicationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private ApplicationEvents applicationEvents;

    @Test
    void should_publishOrderPlacedEvent_when_orderIsPlaced() {
        final var orderId = orderService.placeOrder(new PlaceOrderCommand("SKU-EVT", 4));

        assertThat(applicationEvents.stream(OrderPlaced.class))
                .anySatisfy(event -> {
                    assertThat(event.orderId()).isEqualTo(orderId);
                    assertThat(event.productCode()).isEqualTo("SKU-EVT");
                    assertThat(event.quantity()).isEqualTo(4);
                });
    }
}
