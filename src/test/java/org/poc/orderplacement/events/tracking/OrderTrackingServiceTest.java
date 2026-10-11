package org.poc.orderplacement.events.tracking;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.poc.orderplacement.events.inventory.InventoryQueryService;
import org.poc.orderplacement.events.order.OrderQueryService;
import org.poc.orderplacement.events.order.OrderSummary;
import org.poc.orderplacement.events.shipping.ShippingQueryService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderTrackingServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:30Z");

    @Mock
    private OrderQueryService orderQueryService;

    @Mock
    private InventoryQueryService inventoryQueryService;

    @Mock
    private ShippingQueryService shippingQueryService;

    private OrderTrackingService trackingService;

    @Test
    void should_notBeSettling_when_orderIsFullyProcessed() {
        trackingService = serviceAt(NOW);
        final UUID orderId = UUID.randomUUID();
        givenOrderPlacedAt(orderId, NOW.minusSeconds(5));
        when(inventoryQueryService.isReserved(orderId)).thenReturn(true);
        when(shippingQueryService.isPrepared(orderId)).thenReturn(true);

        final OrderTrackingView view = trackingService.findTracking(orderId).orElseThrow();

        assertThat(view.settling()).isFalse();
    }

    @Test
    void should_beSettling_when_orderIsPendingWithinTheSettlingWindow() {
        trackingService = serviceAt(NOW);
        final UUID orderId = UUID.randomUUID();
        givenOrderPlacedAt(orderId, NOW.minus(OrderTrackingService.SETTLING_WINDOW.minusSeconds(1)));
        when(inventoryQueryService.isReserved(orderId)).thenReturn(false);
        when(shippingQueryService.isPrepared(orderId)).thenReturn(true);

        final OrderTrackingView view = trackingService.findTracking(orderId).orElseThrow();

        assertThat(view.settling()).isTrue();
    }

    @Test
    void should_stopSettling_when_orderHasBeenPendingPastTheSettlingWindow() {
        trackingService = serviceAt(NOW);
        final UUID orderId = UUID.randomUUID();
        givenOrderPlacedAt(orderId, NOW.minus(OrderTrackingService.SETTLING_WINDOW.plusSeconds(1)));
        when(inventoryQueryService.isReserved(orderId)).thenReturn(false);
        when(shippingQueryService.isPrepared(orderId)).thenReturn(true);

        final OrderTrackingView view = trackingService.findTracking(orderId).orElseThrow();

        assertThat(view.settling()).isFalse();
    }

    private void givenOrderPlacedAt(final UUID orderId, final Instant placedAt) {
        when(orderQueryService.findSummary(orderId))
                .thenReturn(Optional.of(new OrderSummary(orderId, "SKU-1", 1, placedAt)));
    }

    private OrderTrackingService serviceAt(final Instant now) {
        return new OrderTrackingService(orderQueryService, inventoryQueryService, shippingQueryService,
                Clock.fixed(now, ZoneOffset.UTC));
    }
}
