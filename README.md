# Distributed Order-Processing System

A microservices order platform built around the **Saga orchestration pattern**. A customer
browses products, places an order, and the order is processed across four services that each
own their own database. Because no single ACID transaction can span those databases, an
**orchestrator** drives the steps and **unwinds them with compensating transactions** if any
step fails.

**Stack:** Java 21 · Spring Boot 3 · Spring Kafka · Spring Data JPA · H2 · Spring Cloud
Gateway · Redis · Resilience4j · React (Vite) · Docker.

> H2 (in-memory, one database per service) is used in place of PostgreSQL so the whole stack
> spins up with zero external DB setup. The design enforces "no shared tables" exactly as
> separate Postgres instances would; swapping in Postgres is a config change.

---

## Architecture

```
                        ┌──────────────┐         ┌─────────────────────────────┐
   Browser  ──REST──▶   │  API Gateway │  ──REST──▶  Order Service (the brain)  │
   (React SPA)          │  JWT • rate  │         │  • order lifecycle           │
                        │  limit • CORS│         │  • SAGA ORCHESTRATOR         │
                        └──────────────┘         │  • saga_state + Outbox       │
                                                 └───────┬─────────────────────┘
                                                         │ Kafka commands / events
                          ┌──────────────────────────────┼───────────────────────────┐
                          ▼                               ▼                            ▼
                 ┌─────────────────┐           ┌──────────────────┐         ┌──────────────────┐
                 │ Inventory Svc   │           │  Payment Svc     │         │  Shipping Svc    │
                 │ products, stock │           │  payments (mock) │         │  shipments       │
                 │ reserve/release │           │  authorize/refund│         │  schedule/cancel │
                 └─────────────────┘           └──────────────────┘         └──────────────────┘
                   own H2 schema                 own H2 schema                own H2 schema
```

- **Frontend → Gateway:** synchronous REST/JSON.
- **Gateway → Order Service:** synchronous REST (the user waits only for the order to be *accepted*).
- **Order Service ↔ business services:** **asynchronous Kafka** commands and events. There are
  **no synchronous service-to-service calls on the saga path.**

Every command/event carries a `sagaId` correlation id so the whole journey is traceable and dedupable.

### The happy path
```
Reserve inventory → Authorize payment → Schedule shipping → Order COMPLETED
```
Order status walks: `CREATED → INVENTORY_RESERVED → PAYMENT_AUTHORIZED → SHIPPING_SCHEDULED → COMPLETED`.

### A failure path (payment declined)
```
Reserve inventory ✓ → Authorize payment ✗
   compensate: Release inventory → Order CANCELLED
```
The orchestrator fires the **inverse commands for steps already completed, in reverse order**,
then marks the order `CANCELLED`. Saga state: `… → COMPENSATING → COMPENSATED`.

---

## How it works (the interesting parts)

| Concern | How it's solved | Where |
|---|---|---|
| **Cross-service consistency** | Saga orchestration with per-step compensation | `order-service` `SagaOrchestrator` |
| **Crash recovery** | `saga_state` row persisted on every transition; a timeout scanner force-compensates sagas stuck awaiting a reply | `SagaState`, `TimeoutScanner` |
| **Dual-write problem** | **Transactional Outbox** — the outgoing command is written to `outbox_event` in the *same* DB transaction as the saga-state change; a relay polls and publishes to Kafka | `OutboxPublisher`, `OutboxRelay` |
| **At-least-once delivery** | **Idempotent consumers** — each consumer dedupes on `sagaId + messageType` (a `processed_message` / `processed_event` ledger, or a unique business key) | every service |
| **Transient payment failures** | **Resilience4j** retry + circuit breaker around the mock gateway | `payment-service` `MockPaymentGateway` |
| **Concurrent orders for scarce stock** | **Optimistic locking** (`@Version`) on the `stock` row | `inventory-service` `Stock` |
| **Security** | JWT issued at the gateway, validated at the edge, identity forwarded as `X-User-Id`; unauthenticated requests rejected before any service sees them | `gateway` `JwtAuthenticationFilter` |
| **Rate limiting** | Redis-backed `RequestRateLimiter` at the gateway | `gateway` `GatewayConfig` |
| **Observability** | `sagaId` in MDC on every log line (greppable journeys) + Micrometer/Prometheus counters (`saga_started/completed/compensated`) | `order-service` |

### Failure-injection levers (for demos)
- **Payment decline:** any order whose **total exceeds `payment.approval-threshold` (default $1000)** is declined → watch inventory get released and the order cancelled.
- **Shipping failure:** a shipping address containing **`FAIL`** makes shipping fail → watch payment refunded *and* inventory released (two-step rollback).
- **Timeout:** kill a business service mid-saga; after `saga.step-timeout` (default 30s) the scanner compensates the stuck saga.

---

## Running it

### Option A — one-command full stack (Docker)
```bash
docker compose -f docker-compose.prod.yml up --build
```
Then open **http://localhost:3000**. Log in with the seeded user **`demo` / `password`**, browse
products, add to cart, check out, and watch the saga progress live. (Gateway is also exposed on
`:8086`, Kafka UI on `:8091`.)

### Option B — infra in Docker, services on your host (dev loop)
```bash
# 1. Start Kafka + Redis
docker compose up -d

# 2. Build everything
mvn -DskipTests package

# 3. Run each service (separate terminals), e.g.:
java -jar order-service/target/order-service-0.1.0.jar
java -jar inventory-service/target/inventory-service-0.1.0.jar
java -jar payment-service/target/payment-service-0.1.0.jar
java -jar shipping-service/target/shipping-service-0.1.0.jar
java -jar gateway/target/gateway-0.1.0.jar

# 4. Frontend
cd frontend && npm install && npm run dev   # http://localhost:5173
```

### Default ports
| Service | Port |
|---|---|
| Gateway (public) | 8080 |
| Inventory | 8081 |
| Order | 8082 |
| Payment | 8083 |
| Shipping | 8084 |
| Frontend (Docker) | 3000 |

---

## Try the saga from the command line
```bash
# Log in (through the gateway) and grab a JWT
TOKEN=$(curl -s localhost:8080/auth/login -H 'Content-Type: application/json' \
  -d '{"username":"demo","password":"password"}' | jq -r .token)

# HAPPY PATH (total < 1000) → COMPLETED
curl -s localhost:8080/api/orders -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"shippingAddress":"221B Baker St","items":[{"productId":1,"quantity":2,"unitPrice":89.99}]}'

# FAILURE PATH (total > 1000) → CANCELLED, inventory released
curl -s localhost:8080/api/orders -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"shippingAddress":"742 Evergreen Tce","items":[{"productId":2,"quantity":4,"unitPrice":329.00}]}'

# Poll status (watch it move through the saga)
curl -s localhost:8080/api/orders/1 -H "Authorization: Bearer $TOKEN"
```

---

## Repository layout
```
order-platform/
├── docker-compose.yml          # dev infra: Kafka (KRaft) + Redis + Kafka UI
├── docker-compose.prod.yml     # full stack: infra + 5 services + frontend
├── Dockerfile                  # one parameterized multi-stage build for all services
├── common-events/              # shared command/event DTOs (plain jar)
├── gateway/                    # Spring Cloud Gateway + JWT auth + rate limiting
├── order-service/              # orchestrator, saga_state, outbox, timeouts, metrics
├── inventory-service/          # products + stock, reserve/release, optimistic locking
├── payment-service/            # mock gateway, authorize/refund, circuit breaker
├── shipping-service/           # schedule/cancel shipments
└── frontend/                   # React + Vite SPA with the live saga stepper
```

## Per-service responsibilities
- **Order Service — the brain.** Accepts orders (202), drives the saga, compensates on failure,
  recovers after a crash, exposes order status. Owns the Outbox.
- **Inventory Service.** Serves the catalog, reserves stock idempotently, releases on
  compensation, handles concurrent reservations via optimistic locking.
- **Payment Service.** Authorizes/refunds idempotently, deterministically declines above a
  threshold, survives redelivered commands, wraps the external call in a circuit breaker.
- **Shipping Service.** Schedules/cancels shipments idempotently. The simplest service.
- **API Gateway.** Owns nothing. Routes, validates JWT, rate-limits — the only public surface.

---

## Notes for production
- Swap H2 for a PostgreSQL instance per service (config only).
- Kafka topics here auto-create with one partition; size partitions/replication for throughput
  and HA in production.
- The JWT secret and any credentials are environment variables — no secrets in the repo.
- At real scale this would split into separate repos plus a shared contracts library
  (`common-events`); the mono-repo keeps local build-and-run a single command.
