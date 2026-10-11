package org.poc.orderplacement.events.inventory;

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
class InventoryServiceTest {

    @Mock
    private InventoryReservationRepository repository;

    @Captor
    private ArgumentCaptor<InventoryReservation> reservationCaptor;

    @InjectMocks
    private InventoryService inventoryService;

    @Test
    void should_persistReservation_when_orderNotYetReserved() {
        final UUID orderId = UUID.randomUUID();
        when(repository.existsByOrderId(orderId)).thenReturn(false);

        inventoryService.reserve(orderId, "SKU-1", 2);

        verify(repository).saveAndFlush(reservationCaptor.capture());
        final InventoryReservation saved = reservationCaptor.getValue();
        assertThat(saved.getOrderId()).isEqualTo(orderId);
        assertThat(saved.getProductCode()).isEqualTo("SKU-1");
        assertThat(saved.getQuantity()).isEqualTo(2);
    }

    @Test
    void should_skipReservation_when_orderAlreadyReserved() {
        final UUID orderId = UUID.randomUUID();
        when(repository.existsByOrderId(orderId)).thenReturn(true);

        inventoryService.reserve(orderId, "SKU-1", 2);

        verify(repository, never()).saveAndFlush(any());
    }
}
