package org.poc.orderplacement.events.inventory;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.poc.orderplacement.events.order.OrderPlaced;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InventoryEventListenerTest {

    @Mock
    private InventoryService inventoryService;

    @InjectMocks
    private InventoryEventListener listener;

    @Test
    void should_delegateToInventoryService_when_orderPlacedEventReceived() {
        final OrderPlaced event = new OrderPlaced(UUID.randomUUID(), "SKU-1", 5, Instant.now());

        listener.onOrderPlaced(event);

        verify(inventoryService).reserve(event.orderId(), event.productCode(), event.quantity());
    }

    @Test
    void should_swallowDataIntegrityViolation_when_orderConcurrentlyReservedByAnotherDelivery() {
        final OrderPlaced event = new OrderPlaced(UUID.randomUUID(), "SKU-1", 5, Instant.now());
        doThrow(new DataIntegrityViolationException("duplicate order_id"))
                .when(inventoryService).reserve(event.orderId(), event.productCode(), event.quantity());

        assertThatCode(() -> listener.onOrderPlaced(event)).doesNotThrowAnyException();
    }
}
