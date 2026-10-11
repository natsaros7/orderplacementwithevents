package org.poc.orderplacement.events.shipping;

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
class ShippingEventListenerTest {

    @Mock
    private ShippingService shippingService;

    @InjectMocks
    private ShippingEventListener listener;

    @Test
    void should_delegateToShippingService_when_orderPlacedEventReceived() {
        final OrderPlaced event = new OrderPlaced(UUID.randomUUID(), "SKU-1", 5, Instant.now());

        listener.onOrderPlaced(event);

        verify(shippingService).prepare(event.orderId());
    }

    @Test
    void should_swallowDataIntegrityViolation_when_orderConcurrentlyPreparedByAnotherDelivery() {
        final OrderPlaced event = new OrderPlaced(UUID.randomUUID(), "SKU-1", 5, Instant.now());
        doThrow(new DataIntegrityViolationException("duplicate order_id"))
                .when(shippingService).prepare(event.orderId());

        assertThatCode(() -> listener.onOrderPlaced(event)).doesNotThrowAnyException();
    }
}
