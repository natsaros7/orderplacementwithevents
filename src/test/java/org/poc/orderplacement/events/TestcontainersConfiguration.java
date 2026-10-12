package org.poc.orderplacement.events;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Shared Postgres container for Testcontainers-based tests. The container is a static singleton
 * so it's started once per JVM and reused across every test context that imports this class,
 * rather than once per Spring context.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer(DockerImageName.parse("postgres:18"));

    static {
        POSTGRES.start();
    }

    @Bean
    @ServiceConnection
    public PostgreSQLContainer postgresContainer() {
        return POSTGRES;
    }
}
