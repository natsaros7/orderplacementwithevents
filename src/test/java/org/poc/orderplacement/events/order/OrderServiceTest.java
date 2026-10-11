package org.poc.orderplacement.events.order;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Captor
    private ArgumentCaptor<OrderPlaced> eventCaptor;

    @InjectMocks
    private OrderService orderService;

    @Test
    void should_saveOrderAndPublishOrderPlaced_when_placingOrder() {
        final PlaceOrderCommand command = new PlaceOrderCommand("SKU-1", 3);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        final UUID orderId = orderService.placeOrder(command);

        verify(orderRepository).save(any(Order.class));
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        final OrderPlaced published = eventCaptor.getValue();
        assertThat(published.orderId()).isEqualTo(orderId);
        assertThat(published.productCode()).isEqualTo("SKU-1");
        assertThat(published.quantity()).isEqualTo(3);
        assertThat(published.placedAt()).isNotNull();
    }
}
