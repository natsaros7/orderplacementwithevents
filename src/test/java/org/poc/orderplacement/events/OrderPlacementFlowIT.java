package org.poc.orderplacement.events;

import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.poc.orderplacement.events.inventory.InventoryReservationRepository;
import org.poc.orderplacement.events.inventory.InventoryService;
import org.poc.orderplacement.events.order.OrderRepository;
import org.poc.orderplacement.events.order.OrderService;
import org.poc.orderplacement.events.order.PlaceOrderCommand;
import org.poc.orderplacement.events.shipping.ShipmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;

/**
 * Proves the full in-process flow: placing an order eventually produces an independent inventory
 * reservation and shipment, and a failure in one listener neither rolls back the order (already
 * committed before AFTER_COMMIT listeners run) nor stops the other, unrelated listener.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
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

    @MockitoSpyBean
    private InventoryService inventoryService;

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

        await().atMost(AWAIT_TIMEOUT).untilAsserted(
                () -> assertThat(shipmentRepository.existsByOrderId(orderId)).isTrue());

        assertThat(orderRepository.findById(orderId)).isPresent();
        assertThat(inventoryReservationRepository.existsByOrderId(orderId)).isFalse();
    }
}
