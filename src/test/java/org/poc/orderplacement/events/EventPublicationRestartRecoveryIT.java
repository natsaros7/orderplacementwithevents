package org.poc.orderplacement.events;

import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.poc.orderplacement.events.inventory.InventoryReservationRepository;
import org.poc.orderplacement.events.inventory.InventoryService;
import org.poc.orderplacement.events.order.OrderRepository;
import org.poc.orderplacement.events.order.OrderService;
import org.poc.orderplacement.events.order.PlaceOrderCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * Proves an incomplete publication survives a restart: a listener fails in this (the "first")
 * application context, as if the process crashed right after, leaving its publication incomplete;
 * starting a second, independent context against the same database with
 * {@code spring.modulith.events.republish-outstanding-events-on-restart=true} — and no injected
 * failure this time — completes it automatically on startup, with no manual resubmission call.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(TestcontainersConfiguration.class)
class EventPublicationRestartRecoveryIT {

    private static final Duration AWAIT_TIMEOUT = Duration.ofSeconds(10);

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private InventoryReservationRepository inventoryReservationRepository;

    @MockitoSpyBean
    private InventoryService inventoryService;

    @Test
    void should_resubmitOutstandingPublication_when_applicationRestarts() throws Exception {
        doThrow(new RuntimeException("simulated crash before processing"))
                .when(inventoryService).reserve(any(), eq("SKU-RESTART"), anyInt());

        final UUID orderId = orderService.placeOrder(new PlaceOrderCommand("SKU-RESTART", 1));
        verify(inventoryService, timeout(AWAIT_TIMEOUT.toMillis())).reserve(eq(orderId), eq("SKU-RESTART"), anyInt());
        assertThat(inventoryReservationRepository.existsByOrderId(orderId)).isFalse();
        assertThat(orderRepository.findById(orderId)).isPresent();

        // Simulate the restart: a brand-new, independent context (no injected failure, no spy)
        // against the same database.
        final PostgreSQLContainer postgres = TestcontainersConfiguration.sharedPostgresContainer();
        try (ConfigurableApplicationContext restarted = new SpringApplicationBuilder(
                        OrderPlacementWithEventsApplication.class)
                .web(WebApplicationType.NONE)
                .properties(
                        // Matches the ${DB_URL:...}/${DB_USER:...}/${DB_PASSWORD:...} placeholders in
                        // application.yml: SpringApplicationBuilder#properties adds *default*
                        // properties (lowest precedence), so overriding spring.datasource.* directly
                        // would lose to application.yml's own (placeholder-resolved) definition of it.
                        "DB_URL=" + postgres.getJdbcUrl(),
                        "DB_USER=" + postgres.getUsername(),
                        "DB_PASSWORD=" + postgres.getPassword(),
                        "spring.docker.compose.enabled=false",
                        "spring.modulith.events.republish-outstanding-events-on-restart=true",
                        // Long enough that only the restart-time republish (not the scheduled
                        // resubmitter) could complete the publication within the await window below.
                        "app.events.resubmission.interval=PT1H",
                        "app.events.resubmission.min-age=PT1H")
                .run()) {

            final InventoryReservationRepository restartedRepository =
                    restarted.getBean(InventoryReservationRepository.class);
            await().atMost(AWAIT_TIMEOUT).untilAsserted(
                    () -> assertThat(restartedRepository.existsByOrderId(orderId)).isTrue());
        }
    }
}
