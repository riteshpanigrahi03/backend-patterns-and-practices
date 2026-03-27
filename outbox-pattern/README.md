# Outbox Pattern Demo

Minimal Spring Boot application that demonstrates the Outbox Pattern.

## Flow

1. `POST /orders` saves a row in the `orders` table.
2. The same transactional method also saves an `ORDER_CREATED` row in the `outbox` table.
3. A scheduled job reads `NEW` outbox rows and publishes them to Kafka topic `order-events`.
4. After a successful publish, the outbox row is marked `PROCESSED`.

## Run

```bash
mvn spring-boot:run
```

## Example Request

```bash
curl -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -d '{"productName":"item","quantity":2}'
```

## Notes

- Database: H2 in-memory
- Kafka broker: `localhost:9092`
- H2 console: `http://localhost:8080/h2-console`
