# order-placement-with-events

In-process domain events POC: placing an order publishes an event that inventory and shipping react to independently.

[![CI](https://github.com/natsaros7/orderplacementwithevents/actions/workflows/ci.yml/badge.svg?branch=master)](https://github.com/natsaros7/orderplacementwithevents/actions/workflows/ci.yml)

## What this POC demonstrates

- An `OrderService` that persists an order and publishes an `OrderPlaced` domain event in the same transaction.
- Two independent listeners (inventory, shipping) that react to that event without knowing about each other.
- `@TransactionalEventListener(phase = AFTER_COMMIT)` + `@Async`: listeners only run once the order is
  durably committed, off the request thread, in their own transaction.
- Idempotent, per-feature persistence: each listener's side effect is safe to retry or redeliver.
- A read model (`OrderStatusView`) assembled from the order plus inventory/shipping's own state, without
  exposing their internals.

```mermaid
sequenceDiagram
    actor User
    participant Controller as OrderController
    participant OrderService
    participant DB as Orders table
    participant Inventory as InventoryEventListener
    participant Shipping as ShippingEventListener

    User->>Controller: POST /orders
    Controller->>OrderService: placeOrder(command)
    OrderService->>DB: save(order)
    OrderService-->>OrderService: publishEvent(OrderPlaced)
    OrderService->>DB: commit
    Controller-->>User: redirect /orders/{id}

    Note over OrderService,Shipping: AFTER_COMMIT, async - runs off the request thread
    OrderService--)Inventory: OrderPlaced
    Inventory->>Inventory: reserve(orderId, productCode, quantity)
    OrderService--)Shipping: OrderPlaced
    Shipping->>Shipping: prepare(orderId)

    User->>Controller: GET /orders/{id}
    Controller-->>User: order + reservation + shipment status
```

## Tech stack

| Component       | Version  |
|------------------|---------|
| Java             | 21      |
| Spring Boot      | 4.1.1   |
| Spring Framework | 7.x     |
| Hibernate ORM    | 7.x     |
| H2 (embedded)    | runtime-managed by Boot BOM |
| Thymeleaf        | Boot-managed |
| JUnit 5 / Mockito / AssertJ | Boot-managed |
| Awaitility       | 4.x     |

## Prerequisites

- JDK 21 (see `.sdkmanrc`; `sdk env install` if using SDKMAN!)
- No local Maven needed — use the bundled wrapper (`./mvnw`)
- Docker, only if you want to build/run the container image

## Build & test

```bash
# Full build: compile, unit + slice tests, integration tests, JaCoCo report
./mvnw clean verify

# Unit and slice tests only (excludes *IT)
./mvnw test

# A single test class
./mvnw test -Dtest=OrderServiceTest

# Coverage report (after a build that ran tests)
open target/site/jacoco/index.html
```

## Run locally

```bash
# Via the wrapper
./mvnw spring-boot:run

# Via the packaged jar
./mvnw clean package -DskipTests
java -jar target/order-placement-with-events-0.0.1-SNAPSHOT.jar

# Via Docker
docker build -t order-placement-with-events .
docker run -p 8080:8080 order-placement-with-events
```

Then open <http://localhost:8080>.

## Configuration

| Env var                   | Property                              | Default                                           | Purpose |
|----------------------------|----------------------------------------|----------------------------------------------------|---------|
| `DB_URL`                  | `spring.datasource.url`               | `jdbc:h2:mem:orderplacement;DB_CLOSE_DELAY=-1`     | H2 JDBC URL |
| `DB_USERNAME`              | `spring.datasource.username`          | `sa`                                               | H2 username |
| `DB_PASSWORD`              | `spring.datasource.password`          | (empty)                                            | H2 password |
| `JPA_DDL_AUTO`             | `spring.jpa.hibernate.ddl-auto`       | `update`                                           | Schema generation strategy |
| `ASYNC_POOL_CORE_SIZE`     | `spring.task.execution.pool.core-size`| `4`                                                | Async executor core pool size |
| `ASYNC_POOL_MAX_SIZE`      | `spring.task.execution.pool.max-size` | `8`                                                | Async executor max pool size |
| `ASYNC_POOL_QUEUE_CAPACITY`| `spring.task.execution.pool.queue-capacity` | `50`                                         | Async executor queue capacity |
| `SERVER_PORT`              | `server.port`                         | `8080`                                             | HTTP port |

The `local` Spring profile additionally enables the H2 console at `/h2-console`.

## Endpoints / UI

| Method | Path             | Purpose |
|--------|------------------|---------|
| GET    | `/`              | Redirects to `/orders/new` |
| GET    | `/orders/new`    | Order placement form |
| POST   | `/orders`        | Places an order; PRG redirect to `/orders/{id}` on success, re-renders the form with errors otherwise |
| GET    | `/orders/{id}`   | Order status (order, inventory reservation, shipment); 404 page for an unknown id |
| GET    | `/actuator/health` | Liveness/readiness probe |
| GET    | `/actuator/info`   | Build/app info |

## Project structure

```
src/main/java/org/poc/orderplacement/events/
├── OrderPlacementWithEventsApplication.java
├── AsyncConfig.java                 # @EnableAsync + Boot's applicationTaskExecutor + exception logging
├── order/                           # Order aggregate, OrderPlaced event, status read model, web layer
│   ├── Order.java, OrderStatus.java, OrderRepository.java
│   ├── PlaceOrderCommand.java, OrderPlaced.java, OrderService.java
│   ├── OrderStatusView.java, OrderStatusQueryService.java, OrderNotFoundException.java
│   └── web/ (HomeController, OrderController, OrderWebExceptionHandler)
├── inventory/                       # Reacts to OrderPlaced; owns its own table
│   ├── InventoryReservation.java, InventoryReservationRepository.java
│   ├── InventoryService.java, InventoryEventListener.java, InventoryQueryService.java
└── shipping/                        # Reacts to OrderPlaced; owns its own table
    ├── Shipment.java, ShipmentRepository.java
    ├── ShippingService.java, ShippingEventListener.java, ShippingQueryService.java
```

## Design notes & trade-offs

| Decision | Why | Trade-off |
|---|---|---|
| In-process `ApplicationEventPublisher` + plain record events | Simplest way to decouple inventory/shipping from order placement in a single-process POC | No cross-process delivery, no durability beyond the JVM — contrast with [orderplacementwithqueue](https://github.com/natsaros7/orderplacementwithqueue) (same domain over JMS) |
| `@TransactionalEventListener(phase = AFTER_COMMIT)` | Listeners never see an order that later rolls back | If the process crashes after commit but before the listener runs, the event is lost — see "Known gaps" |
| `@Async` listeners | Keeps `POST /orders` fast; inventory/shipping failures can't affect the HTTP response | Failures are only visible in logs (via `AsyncUncaughtExceptionHandler`), not to the caller |
| `Propagation.REQUIRES_NEW` per listener | One listener's failure/rollback can't affect the order or the other listener | Two extra connections/transactions per order; acceptable at POC scale |
| IDs (not entities) in `OrderPlaced` | Listeners can't accidentally touch detached JPA state or create implicit coupling to the `order` schema | Listeners that need more order data must query for it themselves |
| Idempotent on `orderId` (unique column + exists-check) | Safe against retry/redelivery without a dedup table | A late duplicate still does one wasted existence check |
| `order` queries inventory/shipping's public query services | Keeps the read model in `order` without reaching into other features' entities | Creates a (deliberate, narrow) cyclic package dependency: `order` → `inventory`/`shipping` for queries, `inventory`/`shipping` → `order` for the event type |

## Known gaps / next steps

- **Lost events on crash**: an `AFTER_COMMIT` event that hasn't yet been delivered when the JVM dies is
  gone. [Spring Modulith](https://docs.spring.io/spring-modulith/reference/)'s event publication registry
  (or a hand-rolled outbox table) would persist the event alongside the order and replay it on restart.
- **No module boundary enforcement**: the `order`/`inventory`/`shipping` separation is convention only.
  Spring Modulith's `ApplicationModules.verify()` / `@ApplicationModuleTest` would make the boundary and
  the allowed event-only coupling testable, not just documented.
- **Single process only**: compare with [orderplacementwithqueue](https://github.com/natsaros7/orderplacementwithqueue),
  which solves the same domain problem over JMS — durable, cross-process delivery, at the cost of needing a
  broker and explicit message contracts instead of in-process event types.
- No authentication/authorization; out of scope for this POC.

## License

MIT — see [LICENSE](LICENSE).
