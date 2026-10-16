# Outbox Pattern Demo (Java / Spring Boot / Kafka / Postgres)

## Architecture

```mermaid
flowchart TD
    %% User & UI
    UI[order-ui<br/>React + Vite]:::client
    UI -->|1. POST /api/orders| API(order-api<br/>Spring Boot WebFlux):::api

    %% Order API writes to primary Postgres + outbox
    API -->|2. INSERT order<br/>INSERT outbox (PENDING)| PG1[(Postgres Primary<br/>port 5432)]:::db

    %% Outbox job reads from replica, publishes to Kafka, writes to primary
    JOB[outbox-job<br/>scheduled poller]:::worker
    PG2[(Postgres Replica<br/>port 5433)]:::db
    KAFKA[(Kafka<br/>order_created topic)]:::msg

    PG1 -.->|replication| PG2
    JOB -->|3. READ pending<br/>from replica| PG2
    JOB -->|4. PUBLISH to| KAFKA
    JOB -->|5. UPDATE status<br/>PUBLISHED| PG1

    %% Order consumer reads Kafka, updates primary Postgres
    CONSUMER[order-consumer<br/>Kafka listener]:::worker
    KAFKA -->|6. CONSUME| CONSUMER
    CONSUMER -->|7. UPDATE status<br/>PROCESSED| PG1

    %% UI reads status via SSE
    UI -->|8. GET /orders/:id| API
    UI -->|9. SSE /orders/:id/status/stream| API
    API -->|10. READ orders| PG1

    classDef client fill:#e1f5fe,stroke:#01579b,stroke-width:2px;
    classDef api fill:#fff3e0,stroke:#e65100,stroke-width:2px;
    classDef db fill:#f3e5f5,stroke:#4a148c,stroke-width:2px;
    classDef msg fill:#e8f5e9,stroke:#1b5e20,stroke-width:2px;
    classDef worker fill:#fce4ec,stroke:#880e4f,stroke-width:2px;
```

## Components

| Component | Responsibility |
|-----------|----------------|
| `order-api` | Spring Boot WebFlux + R2DBC REST API. Creates the order and an outbox record in the same transaction on the **primary** Postgres. Exposes `GET /api/orders/{id}` and `GET /api/orders/{id}/status/stream` (SSE). |
| `outbox-job` | Spring Boot scheduled poller. Reads pending outbox records from the **replica** Postgres, publishes them to the `order_created` Kafka topic, then marks them `PUBLISHED` on the **primary**. |
| `order-consumer` | Spring Boot Kafka consumer. Consumes `order_created` events, marks the order `PROCESSED` (with `processed_at`) on the **primary** Postgres, and exposes data for the UI. |
| `order-ui` | React + Vite frontend. Create order form and order detail page with live SSE status stream (updates until `PROCESSED` or 60s timeout). |

## Data Flow

1. **Create** — User submits order via `order-ui` → `order-api` writes `order` + `outbox` record (status `PENDING`) to primary Postgres in one transaction.
2. **Poll** — `outbox-job` reads `status = 'PENDING'` outbox records from the **replica** (read-only, offloads primary).
3. **Publish** — `outbox-job` publishes each event to Kafka topic `order_created`.
4. **Mark Published** — `outbox-job` updates outbox record to `PUBLISHED` on the **primary**.
5. **Consume** — `order-consumer` reads from Kafka, parses `orderId`, updates `orders` table to `status = 'PROCESSED'` + `processed_at` on the **primary**.
6. **Stream** — `order-ui` opens SSE `GET /api/orders/{id}/status/stream` and watches status until `PROCESSED` or 60s timeout.

## Run infrastructure + apps via Docker

```bash
# Build application jars first
./gradlew clean build -x test

# Start everything
docker compose up -d --build
```

Services:
- Postgres Primary: localhost:5432 (outbox / postgres / postgres)
- Postgres Replica: localhost:5433 (outbox / postgres / postgres)
- Kafka: localhost:9092
- Kafka UI: http://localhost:8088
- order-api: http://localhost:8080
- order-consumer / outbox-job: background services
- order-ui: http://localhost:5173

## Create an order

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"total":123.45,"description":"test order","createdBy":"alice"}'
```

## Local development (without Docker apps)

Use `SPRING_DATASOURCE_URL`, `SPRING_KAFKA_BOOTSTRAP_SERVERS` env vars to point to localhost services, then:

```bash
./gradlew :order-api:bootRun
./gradlew :outbox-job:bootRun
./gradlew :order-consumer:bootRun
```

## React UI

```bash
cd order-ui
npm install
npm run dev
```

Open http://localhost:5173 and create an order. Vite proxies `/api` to http://localhost:8080.

## HTTP requests

- Postman collection: `postman/OrderApi.postman_collection.json`
- VS Code/JetBrains REST requests: `http-requests/create-order.http`
