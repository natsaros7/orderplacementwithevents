package org.poc.orderplacement.events.shipping;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShippingServiceTest {

    @Mock
    private ShipmentRepository repository;

    @Captor
    private ArgumentCaptor<Shipment> shipmentCaptor;

    @InjectMocks
    private ShippingService shippingService;

    @Test
    void should_persistShipment_when_orderNotYetPrepared() {
        final UUID orderId = UUID.randomUUID();
        when(repository.existsByOrderId(orderId)).thenReturn(false);

        shippingService.prepare(orderId);

        verify(repository).saveAndFlush(shipmentCaptor.capture());
        assertThat(shipmentCaptor.getValue().getOrderId()).isEqualTo(orderId);
    }

    @Test
    void should_skipShipment_when_orderAlreadyPrepared() {
        final UUID orderId = UUID.randomUUID();
        when(repository.existsByOrderId(orderId)).thenReturn(true);

        shippingService.prepare(orderId);

        verify(repository, never()).saveAndFlush(any());
    }
}
