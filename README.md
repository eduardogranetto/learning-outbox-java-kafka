# Outbox Pattern Demo (Java / Spring Boot / Kafka / Postgres)

## Components

- `order-api`: Spring Boot (WebFlux + Spring Data JPA) REST API to create orders. Creates the order and an outbox record in the same transaction.
- `outbox-job`: Spring Boot scheduled poller that reads pending outbox records, publishes them to the `order_created` Kafka topic, then marks them `PUBLISHED`.
- `order-consumer`: Spring Boot Kafka consumer that logs messages from `order_created`.

## Run infrastructure + apps via Docker

```bash
# Build application jars first
./gradlew clean build -x test

# Start everything
docker compose up -d --build
```

Services:
- Postgres: localhost:5432 (outbox / postgres / postgres)
- Kafka: localhost:9092
- Kafka UI: http://localhost:8088
- order-api: http://localhost:8080
- order-consumer / outbox-job: background services

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
