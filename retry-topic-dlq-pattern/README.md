# Retry Topic + DLQ Pattern Demo

Spring Boot demo that shows a simple manual Kafka retry-topic flow with a dead-letter queue.

## What This Module Demonstrates

1. Publish an `OrderPlacedEvent` to the main topic `order-events`.
2. Main consumer tries to process the event.
3. When `app.processing.fail-enabled=true`, processing fails and the event is sent to `order-events-retry` with header `x-retry-count=1`.
4. Retry consumer reads the header and tries processing again.
5. If processing still fails and retry count is below the max, it republishes to the retry topic with an incremented header.
6. If max retries are reached, the event is sent to `order-events-dlq`.
7. DLQ consumer logs the failed event.
8. When `app.processing.fail-enabled=false`, processing succeeds and the event is saved to H2.

This module keeps the retry logic manual and explicit so the flow is easy to follow for beginners.

## Topics

- Main topic: `order-events`
- Retry topic: `order-events-retry`
- DLQ topic: `order-events-dlq`

## Event Shape

`OrderPlacedEvent`

- `eventId`
- `orderId`
- `customerId`
- `amount`

## Project Structure

```text
retry-topic-dlq-pattern
├── pom.xml
├── README.md
└── src
    └── main
        ├── java
        │   └── org/example/retrydlq
        │       ├── RetryTopicDlqPatternApplication.java
        │       ├── config
        │       │   └── KafkaTopicConfig.java
        │       ├── controller
        │       │   └── OrderController.java
        │       ├── domain
        │       │   └── ProcessedOrder.java
        │       ├── dto
        │       │   ├── CreateOrderRequest.java
        │       │   ├── EventPublishResponse.java
        │       │   └── OrderPlacedEvent.java
        │       ├── repository
        │       │   └── ProcessedOrderRepository.java
        │       └── service
        │           └── KafkaRetryDemoService.java
        └── resources
            └── application.yml
```

## Class Responsibilities

- `RetryTopicDlqPatternApplication`: starts the Spring Boot app.
- `KafkaTopicConfig`: creates the main, retry, and DLQ Kafka topics.
- `OrderController`: exposes `POST /api/events/orders`.
- `KafkaRetryDemoService`: contains the full demo flow: publishing, main consumer, retry consumer, DLQ consumer, retry header handling, and processing logic.
- `OrderPlacedEvent`: Kafka message payload.
- `CreateOrderRequest`: request body for `POST /api/events/orders`.
- `EventPublishResponse`: response returned after the event is published to the main Kafka topic.
- `ProcessedOrder`: H2 table row created after successful processing.
- `ProcessedOrderRepository`: writes successful processed orders.

## Prerequisites

- Java 17
- Maven
- Kafka running on `localhost:9092`

## Run The App

From the repo root:

```bash
mvn -pl retry-topic-dlq-pattern spring-boot:run
```

App URLs:

- API: `http://localhost:8082`
- H2 console: `http://localhost:8082/h2-console`

## APIs

### 1. Publish a custom event

```bash
curl -X POST http://localhost:8082/api/events/orders \
  -H "Content-Type: application/json" \
  -d '{
    "orderId":"order-1001",
    "customerId":"customer-42",
    "amount":499.99
  }'
```

Response:

```json
{
  "message": "Order event published to Kafka for asynchronous processing",
  "eventId": "generated-event-id",
  "topic": "order-events"
}
```

## Expected Flow

### `app.processing.fail-enabled=false`

- Main consumer receives the event
- Processing succeeds immediately
- A `processed_order` row is created in H2

### `app.processing.fail-enabled=true`

- Main consumer fails
- Retry consumer keeps retrying until `app.retry.max-attempts`
- Event is sent to DLQ
- DLQ consumer logs the failed event

## Important Properties

```yaml
app:
  retry:
    max-attempts: 3
  processing:
    fail-enabled: true
  topics:
    main: order-events
    retry: order-events-retry
    dlq: order-events-dlq
```

## Logging To Watch

The demo logs these steps clearly:

- received event
- processing event
- failure occurred
- sending to retry topic
- retry attempt count
- max retries reached
- sending to DLQ
- received in DLQ

## Build

```bash
mvn -pl retry-topic-dlq-pattern test
```
