package org.poc.orderplacement.events;

import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.poc.orderplacement.events.inventory.InventoryReservationRepository;
import org.poc.orderplacement.events.inventory.InventoryService;
import org.poc.orderplacement.events.order.OrderPlaced;
import org.poc.orderplacement.events.order.OrderRepository;
import org.poc.orderplacement.events.order.OrderService;
import org.poc.orderplacement.events.order.PlaceOrderCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.modulith.events.IncompleteEventPublications;
import org.springframework.modulith.events.core.EventPublicationRegistry;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * Proves the event publication registry is what actually closes the "AFTER_COMMIT event lost on
 * crash" gap: a listener failure leaves its publication incomplete (durably, in the
 * {@code event_publication} table) rather than just logging and forgetting, the order that
 * triggered it is never rolled back, and resubmitting the incomplete publication completes it.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(TestcontainersConfiguration.class)
class EventPublicationRegistryIT {

    private static final Duration AWAIT_TIMEOUT = Duration.ofSeconds(5);

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private InventoryReservationRepository inventoryReservationRepository;

    @Autowired
    private EventPublicationRegistry eventPublicationRegistry;

    @Autowired
    private IncompleteEventPublications incompleteEventPublications;

    @MockitoSpyBean
    private InventoryService inventoryService;

    @Test
    void should_leavePublicationIncomplete_then_completeItOnResubmission_withoutRollingBackTheOrder()
            throws Exception {
        doThrow(new RuntimeException("simulated listener failure"))
                .when(inventoryService).reserve(any(), eq("SKU-REGISTRY"), anyInt());

        final UUID orderId = orderService.placeOrder(new PlaceOrderCommand("SKU-REGISTRY", 1));

        // Wait for the failing invocation itself, then for Modulith to record it as incomplete.
        verify(inventoryService, timeout(AWAIT_TIMEOUT.toMillis())).reserve(eq(orderId), eq("SKU-REGISTRY"), anyInt());
        await().atMost(AWAIT_TIMEOUT).untilAsserted(() -> assertThat(eventPublicationRegistry.findIncompletePublications())
                .anyMatch(publication -> isOrderPlacedFor(publication.getEvent(), orderId)));

        // The order's own transaction already committed before the AFTER_COMMIT listener ran.
        assertThat(orderRepository.findById(orderId)).isPresent();
        assertThat(inventoryReservationRepository.existsByOrderId(orderId)).isFalse();

        // Let the next attempt succeed, then resubmit — exactly what the scheduled
        // StaleEventPublicationResubmitter (and a restart, via republish-outstanding-events-on-restart)
        // would do automatically.
        doCallRealMethod().when(inventoryService).reserve(any(), eq("SKU-REGISTRY"), anyInt());
        incompleteEventPublications.resubmitIncompletePublicationsOlderThan(Duration.ZERO);

        await().atMost(AWAIT_TIMEOUT).untilAsserted(
                () -> assertThat(inventoryReservationRepository.existsByOrderId(orderId)).isTrue());
        await().atMost(AWAIT_TIMEOUT).untilAsserted(() -> assertThat(eventPublicationRegistry.findIncompletePublications())
                .noneMatch(publication -> isOrderPlacedFor(publication.getEvent(), orderId)));
    }

    private static boolean isOrderPlacedFor(final Object event, final UUID orderId) {
        return event instanceof OrderPlaced orderPlaced && orderPlaced.orderId().equals(orderId);
    }
}
