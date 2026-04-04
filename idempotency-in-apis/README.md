# API Idempotency Demo

Simple Spring Boot demo project that explains API idempotency with a payment example.

## Tech Stack

- Java 21
- Spring Boot
- Spring Web
- Spring Data JPA
- H2 in-memory database
- Maven

## What This Demo Shows

`POST /payments` accepts:

- `Idempotency-Key` header
- request body with `orderId` and `amount`

Behavior:

1. New idempotency key creates a payment and stores an idempotency record.
2. Same key with same payload returns the existing payment response.
3. Same key with different payload returns a conflict error.

## Project Structure

```text
src/main/java/org/example
├── controller
├── dto
├── entity
├── exception
├── repository
├── service
└── util
```

## How to Run

```bash
mvn spring-boot:run
```

Application starts on `http://localhost:8080`.

Useful links:

- API: `http://localhost:8080/payments`
- H2 Console: `http://localhost:8080/h2-console`

H2 connection details:

- JDBC URL: `jdbc:h2:mem:idempotencydb`
- Username: `sa`
- Password: leave blank

## Sample Requests

### 1. New key + new request

```bash
curl --location 'http://localhost:8080/payments' \
--header 'Content-Type: application/json' \
--header 'Idempotency-Key: abc-123' \
--data '{
  "orderId": "ORD-101",
  "amount": 500
}'
```

Sample response:

```json
{
  "paymentRef": "PAY-1001",
  "orderId": "ORD-101",
  "amount": 500,
  "status": "SUCCESS"
}
```

### 2. Same key + same payload

Run the same request again:

```bash
curl --location 'http://localhost:8080/payments' \
--header 'Content-Type: application/json' \
--header 'Idempotency-Key: abc-123' \
--data '{
  "orderId": "ORD-101",
  "amount": 500
}'
```

The API returns the same payment response without creating a duplicate payment.

### 3. Same key + different payload

```bash
curl --location 'http://localhost:8080/payments' \
--header 'Content-Type: application/json' \
--header 'Idempotency-Key: abc-123' \
--data '{
  "orderId": "ORD-101",
  "amount": 700
}'
```

Sample response:

```json
{
  "message": "Idempotency key already used with different request payload",
  "timestamp": "2024-01-01T10:00:00"
}
```

## Notes

- The request hash is generated with SHA-256 using `orderId` and `amount`.
- Payment references are generated as `PAY-1001`, `PAY-1002`, and so on.
- Payment status is fixed as `SUCCESS` for simplicity.
