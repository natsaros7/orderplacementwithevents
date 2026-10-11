# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Bounded, named async executor (`spring.task.execution.*`) with a logging `AsyncUncaughtExceptionHandler`.
- `InventoryReservation` and `Shipment` as independent, idempotent read/write models owned by their own features.
- `OrderStatusView` read model assembling order, inventory and shipping state for the order detail page.
- Bean Validation on order placement, with inline form errors.
- Unit, slice (`@WebMvcTest`, `@DataJpaTest`), event-publication (`@RecordApplicationEvents`) and
  end-to-end (`@SpringBootTest` + Awaitility) test coverage.
- Actuator `health`/`info` endpoints, `application.yml` with externalised configuration.
- `Dockerfile` (multi-stage, layered jar), CI workflow, Dependabot config, repo hygiene files.

### Changed
- Upgraded to Spring Boot 4.1.1 and Java 21; renamed starters to their Boot 4 equivalents
  (`spring-boot-starter-webmvc`, `-data-jpa`, `-thymeleaf`, `-validation`, `-actuator`).
- Replaced the `ApplicationEvent` subclass carrying the `Order` entity with a plain `OrderPlaced` record
  carrying only the order id and the facts listeners need.
- Inventory and shipping listeners are now `@Async` `@TransactionalEventListener(phase = AFTER_COMMIT)`,
  each persisting its own state in `REQUIRES_NEW` transactions instead of just logging to stdout.
- Reworked the web flow: `GET /orders/new` (form) → `POST /orders` (PRG redirect) → `GET /orders/{id}`
  (status page), replacing the broken `/inventory` and `/shipping` pages that lost the order id on redirect.
- Renamed the `shiping` package to `shipping` and reorganised the codebase package-by-feature under
  `org.poc.orderplacement.events`.
- Replaced `System.out` logging with SLF4J.

### Removed
- The unreachable `/shipping` page and the inventory/shipping pages that didn't display real order state.

### Fixed
- `orderId` model attribute no longer lost across the post-redirect-get cycle.
