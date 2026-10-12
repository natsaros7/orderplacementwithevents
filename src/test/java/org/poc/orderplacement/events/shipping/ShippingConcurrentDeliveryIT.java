package org.poc.orderplacement.events.shipping;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.Test;
import org.poc.orderplacement.events.TestcontainersConfiguration;
import org.poc.orderplacement.events.order.OrderPlaced;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Proves that two concurrently redelivered copies of the same event never create two shipments.
 * Each publish runs in its own, separately-committed transaction (simulating genuine redelivery of
 * the same {@code OrderPlaced}), so Modulith's event publication registry sees two real,
 * independent publications of it — each with its own {@code AFTER_COMMIT}, {@code @Async} dispatch
 * — rather than calling the listener method directly, which would bypass the registry entirely.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(TestcontainersConfiguration.class)
class ShippingConcurrentDeliveryIT {

    @Autowired
    private ShipmentRepository repository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void should_persistExactlyOneShipment_when_sameOrderDeliveredConcurrently() throws Exception {
        final TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        final OrderPlaced event = new OrderPlaced(UUID.randomUUID(), "SKU-RACE", 1, Instant.now());
        final CountDownLatch startGate = new CountDownLatch(1);

        final Runnable publish = () -> {
            try {
                startGate.await();
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            transactionTemplate.executeWithoutResult(status -> eventPublisher.publishEvent(event));
        };
        final Thread first = new Thread(publish);
        final Thread second = new Thread(publish);
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
        assertThat(repository.findAll().stream().filter(s -> s.getOrderId().equals(event.orderId())).count())
                .isEqualTo(1);
    }
}
