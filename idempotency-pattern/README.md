# Idempotency Pattern Demo

Spring Boot demo that shows why a Kafka consumer needs idempotency when handling duplicate `OrderPlacedEvent` deliveries.

## Business Flow

1. An order is placed and an `OrderPlacedEvent` is published to Kafka.
2. `LoyaltyPointsService` consumes the event.
3. The service adds reward points to the `reward_ledger` table.
4. In the idempotent flow, the same transaction also records the `eventId` in `processed_events`.

## What This Module Demonstrates

### 1. Non-idempotent flow

`processWithoutIdempotency()` saves reward points every time the consumer sees the event.

If Kafka redelivers the same message, the customer receives points multiple times.

### 2. Idempotent flow

`processWithIdempotency()`:

- checks whether `eventId` already exists in `processed_events`
- skips duplicate processing when the event is already present
- saves both the reward entry and processed event marker inside the same transaction

Switch between the two modes with:

```properties
app.loyalty.idempotency-enabled=false
```

or:

```properties
app.loyalty.idempotency-enabled=true
```

Default is `false` so the duplicate-processing problem is visible first.

## Run Kafka

This module expects Kafka on `localhost:9092`.

One option is Docker:

```bash
docker run --name idempotency-kafka -p 9092:9092 \
  -e KAFKA_NODE_ID=1 \
  -e KAFKA_PROCESS_ROLES=broker,controller \
  -e KAFKA_LISTENERS=PLAINTEXT://:9092,CONTROLLER://:9093 \
  -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092 \
  -e KAFKA_CONTROLLER_LISTENER_NAMES=CONTROLLER \
  -e KAFKA_LISTENER_SECURITY_PROTOCOL_MAP=CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT \
  -e KAFKA_CONTROLLER_QUORUM_VOTERS=1@localhost:9093 \
  -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 \
  -e KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR=1 \
  -e KAFKA_TRANSACTION_STATE_LOG_MIN_ISR=1 \
  apache/kafka:3.7.1
```

## Start the Application

From the repo root:

```bash
mvn -pl idempotency-pattern spring-boot:run
```

App URLs:

- API: `http://localhost:8081`
- H2 console: `http://localhost:8081/h2-console`

## Demo Requests

Publish one event:

```bash
curl -X POST http://localhost:8081/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "orderId":"order-1001",
    "customerId":"customer-42",
    "orderTotal":250.00,
    "rewardPoints":25
  }'
```

Publish the exact same event 3 times:

```bash
curl -X POST "http://localhost:8081/api/orders/duplicate?deliveries=3" \
  -H "Content-Type: application/json" \
  -d '{
    "eventId":"evt-order-1001",
    "orderId":"order-1001",
    "customerId":"customer-42",
    "orderTotal":250.00,
    "rewardPoints":25
  }'
```

Inspect the reward ledger:

```bash
curl http://localhost:8081/api/loyalty/ledger
```

Inspect processed events:

```bash
curl http://localhost:8081/api/loyalty/processed-events
```

## Expected Result

- When `app.loyalty.idempotency-enabled=false`, the same event creates multiple rows in `reward_ledger`.
- When `app.loyalty.idempotency-enabled=true`, the duplicate deliveries create only one reward entry and one `processed_events` row.

## Tests

```bash
mvn -pl idempotency-pattern test
```
