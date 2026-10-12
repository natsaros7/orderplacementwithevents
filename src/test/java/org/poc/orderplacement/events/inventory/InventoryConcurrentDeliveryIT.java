package org.poc.orderplacement.events.inventory;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.Test;
import org.poc.orderplacement.events.TestcontainersConfiguration;
import org.poc.orderplacement.events.order.OrderPlaced;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Proves that two concurrently delivered copies of the same event never create two reservations.
 * {@code onOrderPlaced} is {@code @Async}, so calling it (even from outside the bean, as here)
 * only dispatches to the executor and returns immediately — the assertion has to poll for the
 * eventual, settled state rather than join on the calls themselves.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(TestcontainersConfiguration.class)
class InventoryConcurrentDeliveryIT {

    @Autowired
    private InventoryEventListener listener;

    @Autowired
    private InventoryReservationRepository repository;

    @Test
    void should_persistExactlyOneReservation_when_sameOrderDeliveredConcurrently() throws Exception {
        final OrderPlaced event = new OrderPlaced(UUID.randomUUID(), "SKU-RACE", 1, Instant.now());
        final CountDownLatch startGate = new CountDownLatch(1);

        final Runnable deliver = () -> {
            try {
                startGate.await();
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            listener.onOrderPlaced(event);
        };
        final Thread first = new Thread(deliver);
        final Thread second = new Thread(deliver);
        first.start();
        second.start();
        startGate.countDown();
        first.join();
        second.join();

        await().atMost(Duration.ofSeconds(5)).untilAsserted(
                () -> assertThat(repository.existsByOrderId(event.orderId())).isTrue());

        // Give a losing duplicate a chance to land before asserting there's still exactly one row.
        Thread.sleep(200);
        assertThat(repository.findByOrderId(event.orderId())).isPresent();
        assertThat(repository.findAll().stream().filter(r -> r.getOrderId().equals(event.orderId())).count())
                .isEqualTo(1);
    }
}
