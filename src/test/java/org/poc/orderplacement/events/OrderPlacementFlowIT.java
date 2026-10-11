package org.poc.orderplacement.events;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.poc.orderplacement.events.inventory.InventoryReservationRepository;
import org.poc.orderplacement.events.inventory.InventoryService;
import org.poc.orderplacement.events.order.OrderPlaced;
import org.poc.orderplacement.events.order.OrderRepository;
import org.poc.orderplacement.events.order.OrderService;
import org.poc.orderplacement.events.order.PlaceOrderCommand;
import org.poc.orderplacement.events.shipping.ShipmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * Proves the full in-process flow: placing an order eventually produces an independent inventory
 * reservation and shipment, a failure in one listener neither rolls back the order (already
 * committed before AFTER_COMMIT listeners run) nor stops the other, unrelated listener, a rolled
 * back order never fires any listener, and the listeners genuinely run off the request thread.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@RecordApplicationEvents
class OrderPlacementFlowIT {

    private static final Duration AWAIT_TIMEOUT = Duration.ofSeconds(5);

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private InventoryReservationRepository inventoryReservationRepository;

    @Autowired
    private ShipmentRepository shipmentRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private ApplicationEvents applicationEvents;

    @MockitoSpyBean
    private InventoryService inventoryService;

    @Test
    void should_publishOrderPlacedEvent_when_orderIsPlaced() {
        final UUID orderId = orderService.placeOrder(new PlaceOrderCommand("SKU-EVT", 4));

        assertThat(applicationEvents.stream(OrderPlaced.class))
                .anySatisfy(event -> {
                    assertThat(event.orderId()).isEqualTo(orderId);
                    assertThat(event.productCode()).isEqualTo("SKU-EVT");
                    assertThat(event.quantity()).isEqualTo(4);
                });
    }

    @Test
    void should_createInventoryReservationAndShipment_when_orderIsPlaced() {
        final UUID orderId = orderService.placeOrder(new PlaceOrderCommand("SKU-FLOW-OK", 2));

        await().atMost(AWAIT_TIMEOUT).untilAsserted(() -> {
            assertThat(inventoryReservationRepository.existsByOrderId(orderId)).isTrue();
            assertThat(shipmentRepository.existsByOrderId(orderId)).isTrue();
        });
    }

    @Test
    void should_notRollBackOrderOrStopShipping_when_inventoryListenerFails() {
        doThrow(new RuntimeException("simulated inventory failure"))
                .when(inventoryService).reserve(any(), eq("SKU-FLOW-FAIL"), anyInt());

        final UUID orderId = orderService.placeOrder(new PlaceOrderCommand("SKU-FLOW-FAIL", 1));

        // Wait for the failing listener invocation itself, not just the unrelated shipment, so the
        // test would fail loudly if the failure injection stopped actually firing.
        verify(inventoryService, timeout(AWAIT_TIMEOUT.toMillis())).reserve(eq(orderId), eq("SKU-FLOW-FAIL"), anyInt());
        await().atMost(AWAIT_TIMEOUT).untilAsserted(
                () -> assertThat(shipmentRepository.existsByOrderId(orderId)).isTrue());

        assertThat(orderRepository.findById(orderId)).isPresent();
        assertThat(inventoryReservationRepository.existsByOrderId(orderId)).isFalse();
    }

    @Test
    void should_neverFireListeners_when_orderTransactionRollsBack() {
        final TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        final UUID orderId = transactionTemplate.execute(status -> {
            final UUID id = orderService.placeOrder(new PlaceOrderCommand("SKU-FLOW-ROLLBACK", 1));
            status.setRollbackOnly();
            return id;
        });

        // AFTER_COMMIT never fires for a rolled-back transaction, so there's nothing to await;
        // give any wrongly-firing listener a window to show up instead.
        verify(inventoryService, after(500).never()).reserve(any(), eq("SKU-FLOW-ROLLBACK"), anyInt());
        assertThat(orderRepository.findById(orderId)).isEmpty();
        assertThat(inventoryReservationRepository.existsByOrderId(orderId)).isFalse();
        assertThat(shipmentRepository.existsByOrderId(orderId)).isFalse();
    }

    @Test
    void should_runListenerOffTheRequestThread_when_orderIsPlaced() {
        final String callingThreadName = Thread.currentThread().getName();
        final AtomicReference<String> listenerThreadName = new AtomicReference<>();
        doAnswer(invocation -> {
            listenerThreadName.set(Thread.currentThread().getName());
            return invocation.callRealMethod();
        }).when(inventoryService).reserve(any(), eq("SKU-FLOW-THREAD"), anyInt());

        final UUID orderId = orderService.placeOrder(new PlaceOrderCommand("SKU-FLOW-THREAD", 1));

        await().atMost(AWAIT_TIMEOUT).untilAsserted(
                () -> assertThat(inventoryReservationRepository.existsByOrderId(orderId)).isTrue());
        assertThat(listenerThreadName.get()).isNotNull().isNotEqualTo(callingThreadName)
                .startsWith("events-async-");
    }
}
