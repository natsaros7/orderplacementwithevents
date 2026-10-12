# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Bounded, named async executor (`spring.task.execution.*`) with a logging `AsyncUncaughtExceptionHandler`
  and a `CallerRunsPolicy` rejection handler so pool saturation degrades to synchronous execution instead
  of surfacing a 500 for an order that's already committed.
- A page-settling flag (`OrderTrackingView.settling`) so the order status page stops auto-refreshing once
  a pending order has been waiting past a 30s window instead of polling forever.
- `InventoryReservation` and `Shipment` as independent, idempotent read/write models owned by their own features.
- A `tracking` feature owning the `OrderTrackingView` read model, assembled from `order`'s, `inventory`'s and
  `shipping`'s narrow query services; `order` itself has no dependency on the other features.
- An ArchUnit test (`ArchitectureTest`) guarding the package-by-feature boundary: no cycles between
  feature packages, and `order` must not depend on `inventory`/`shipping`/`tracking`.
- Bean Validation on order placement, with inline form errors.
- Unit, slice (`@WebMvcTest`, `@DataJpaTest`), event-publication (`@RecordApplicationEvents`),
  concurrent-delivery, and end-to-end (`@SpringBootTest` + Awaitility) test coverage.
- Actuator `health`/`info` endpoints, `application.yml` with externalised configuration.
- `Dockerfile` (multi-stage, layered jar), CI workflow, Dependabot config, repo hygiene files.
- PostgreSQL + Flyway: `db/migration/V1__create_schema.sql` creates `orders`, `inventory_reservations` and
  `shipments` with proper column types (`uuid`, `timestamptz`), a `status` check constraint and unique
  constraints/indexes on `order_id`.
- `compose.yaml` (Postgres only) plus `spring-boot-docker-compose` so `./mvnw spring-boot:run` /
  `java -jar ...` auto-start and auto-stop it.
- A shared, JVM-wide Testcontainers Postgres container (`TestcontainersConfiguration`, `@ServiceConnection`)
  for `@DataJpaTest` (`@AutoConfigureTestDatabase(replace = NONE)`) and `@SpringBootTest` ITs.

### Changed
- Upgraded to Spring Boot 4.1.1 and Java 25; renamed starters to their Boot 4 equivalents
  (`spring-boot-starter-webmvc`, `-data-jpa`, `-thymeleaf`, `-validation`, `-actuator`).
- Upgraded to Maven wrapper 3.10.0.
- Replaced the `ApplicationEvent` subclass carrying the `Order` entity with a plain `OrderPlaced` record
  carrying only the order id and the facts listeners need.
- Inventory and shipping listeners are now `@Async` `@TransactionalEventListener(phase = AFTER_COMMIT)`,
  each persisting its own state in `REQUIRES_NEW` transactions instead of just logging to stdout.
- Reworked the web flow: `GET /orders/new` (form) → `POST /orders` (PRG redirect) → `GET /orders/{id}`
  (status page), replacing the broken `/inventory` and `/shipping` pages that lost the order id on redirect.
- Renamed the `shiping` package to `shipping` and reorganised the codebase package-by-feature under
  `org.poc.orderplacement.events`.
- Replaced `System.out` logging with SLF4J.
- `spring.jpa.hibernate.ddl-auto` is now fixed at `validate`; Flyway migrations are the only way to change
  the schema (previously `update`, configurable via the now-removed `JPA_DDL_AUTO` env var).

### Removed
- The unreachable `/shipping` page and the inventory/shipping pages that didn't display real order state.
- H2 entirely, including the `local` profile that enabled its console. Replaced by PostgreSQL everywhere
  (dev via `compose.yaml`, tests via Testcontainers).

### Fixed
- `orderId` model attribute no longer lost across the post-redirect-get cycle.
- Concurrent/duplicate `OrderPlaced` delivery no longer logs a spurious async-exception ERROR: the
  unique-constraint violation is now surfaced inside the `REQUIRES_NEW` transaction (via
  `saveAndFlush`) and caught by the listener as a benign duplicate.
- Dockerfile layered-jar image now starts: the jar is renamed to `application.jar` before
  `jarmode=tools extract`, matching the `ENTRYPOINT`.
