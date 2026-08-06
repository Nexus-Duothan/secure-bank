# Payments Service (`payments-service`)

The **Payments Service** processes Account-to-Vendor (A2V) external payments, merchant settlement, QR code transactions, and digital receipts. Built on Spring Boot 3.3.2 and JDK 21 LTS, it validates and holds the JWTs issued by `auth-service` (sharing its `jwt.secret`, no local login of its own) and delegates fraud detection to the real, already-built `security/audit-recovery-service`.

---

## 🎯 Implemented Features & SRS Mapping

- **Merchant Registration**: Self-service registration for users with the `MERCHANT` role — merchants are users, not a separate identity system (`auth-service`'s `Role` enum). Generates a unique `merchant_code` (e.g. `MCH-7F3K9Q`).
- **External Merchant Payments (A2V)**: `POST /pay` settles a customer payment against a registered, active merchant.
- **QR-Based Payments**: `POST /qr/pay` decodes a base64-encoded JSON QR payload (a format invented for this service — see below) and settles against the embedded or overridden amount.
- **Digital Receipts**: Every completed payment gets a SHA-256-derived, cryptographically reference-numbered receipt (`RCPT-XXXXXXXXXXXXXXXX`), retrievable via `GET /{id}/receipt`.
- **Fraud Detection Hook**: After every vendor payment, this service posts an audit entry to `security/audit-recovery-service` (Rust/Axum, port `8089`) and checks its live anomaly report. If the payer trips the high-velocity threshold (≥10 events in a rolling 1-hour window, `risk_score ≥ 75`), the payment is flipped from `COMPLETED` to `HELD_FOR_REVIEW` for a `BANK_OFFICER`/`ADMIN` to resolve via `POST /officer/{id}/review`.
- **Event Publishing**: Publishes `PaymentCompletedEvent`/`PaymentHeldEvent` to Kafka (`payments.completed.v1`, `payments.held-for-review.v1`) — see "Kafka Event Schema" below. No consumer exists yet anywhere in the repo; this is publish-only groundwork.

---

## 📡 REST API Endpoint Specification

All endpoints are exposed under `/api/v1/payments`:

| Method | Endpoint                               | Authorization                  | Description                                                            |
| :----- | :------------------------------------- | :----------------------------- | :--------------------------------------------------------------------- |
| `POST` | `/api/v1/payments/pay`                 | Authenticated                  | Pay a registered merchant directly by `merchantCode`.                  |
| `POST` | `/api/v1/payments/qr/pay`              | Authenticated                  | Pay by scanning/submitting a merchant QR payload.                      |
| `GET`  | `/api/v1/payments/{id}`                | Owner / `BANK_OFFICER`/`ADMIN` | Fetch a single payment.                                                |
| `GET`  | `/api/v1/payments/{id}/receipt`        | Owner                          | Fetch the digital receipt for a completed/held payment.                |
| `GET`  | `/api/v1/payments`                     | Authenticated                  | Paginated payment history for the caller (`?status=` filter optional). |
| `POST` | `/api/v1/payments/merchants/register`  | `MERCHANT`                     | Self-service merchant registration (one profile per user).             |
| `GET`  | `/api/v1/payments/merchants/{code}`    | Authenticated                  | Look up an active merchant by code before paying.                      |
| `GET`  | `/api/v1/payments/officer/held`        | `BANK_OFFICER` / `ADMIN`       | List all payments currently `HELD_FOR_REVIEW`.                         |
| `POST` | `/api/v1/payments/officer/{id}/review` | `BANK_OFFICER` / `ADMIN`       | Approve (→ `COMPLETED`) or decline (→ `DECLINED`) a held payment.      |

---

## 🔌 New Integration Conventions Introduced by This Service

No prior service in this repo calls another service over HTTP or publishes to Kafka, so the following are new, documented-here conventions rather than established patterns being followed:

### Accounts Service Integration

`accounts-service` owns the ledger. A vendor payment names the payer rather than an account, so the debit is addressed by customer and resolved there to that customer's primary account:

```
POST http://localhost:8084/internal/v1/accounts/by-user/{userId}/debit
Body: { "amount": 1500.00, "currency": "LKR", "reference": "<paymentId>",
        "merchant": "...", "category": "...", "transactionType": "CARD_PAYMENT" }

200 -> { "accountId": "...", "newBalance": 46731.76 }   (debit applied)
404 -> the customer has no account
409 -> insufficient funds, or the account is frozen
```

The route lives under `/internal`, which the API gateway does not publish, so only services inside the cluster can move money this way. `reference` is the payment id: re-posting it is a no-op, so a retried call after a timeout cannot charge the customer twice.

The debit call is load-bearing: it is never skipped, and its failure is never silently swallowed (it surfaces as `503`), because money must actually move before a payment is marked `COMPLETED`.

**Known limitation**: if a payment is flagged as high-velocity _after_ the debit already succeeded, there is currently no reversal — the payment is held for officer review, but the debited funds are not automatically returned. `POST /internal/v1/accounts/{id}/credit` now exists to build that on.

### Audit & Fraud Hook (real, already built)

`AuditRecoveryClient` calls the real `security/audit-recovery-service`:

```
POST http://127.0.0.1:8089/api/v1/audit/entries
Header: X-Internal-Service-Key: securebank_audit_internal_secret_key_2026
Body: { "service_name": "payments-service", "event_type": "VENDOR_PAYMENT",
        "user_id": "<payerUserId>", "payload": { ... } }

GET  http://127.0.0.1:8089/api/v1/audit/anomalies
Header: X-Internal-Service-Key: securebank_audit_internal_secret_key_2026
```

`GET /anomalies` has no server-side `user_id` filter — it returns every currently-`ACTIVE` anomaly report, so this client filters client-side. Failures talking to `audit-recovery-service` are logged and swallowed (fail-open): a down fraud service should not block a payment whose debit already cleared.

### QR Payload Format (invented — no prior convention)

A SecureBank merchant QR encodes a base64 JSON object:

```json
{ "merchantCode": "MCH-7F3K9Q", "suggestedAmount": 1500.0, "currency": "LKR" }
```

`POST /qr/pay` decodes this, uses `suggestedAmount`/`currency` unless the request body overrides `amount`. The frontend must generate QR codes in this exact shape.

### Kafka Event Schema (invented — no prior convention, publish-only)

| Topic                         | Payload (`PaymentCompletedEvent` / `PaymentHeldEvent`)                              |
| :---------------------------- | :---------------------------------------------------------------------------------- |
| `payments.completed.v1`       | `paymentId, payerUserId, merchantId, amount, currency, referenceNumber, occurredAt` |
| `payments.held-for-review.v1` | `paymentId, payerUserId, merchantId, amount, reason, occurredAt`                    |

No consumer exists in the repo yet (including `notification-service`, also unbuilt) — this is groundwork for one.

---

## ⚙️ Environment Configuration (`application.yml`)

- `server.port`: `8086`
- `spring.datasource.url`: `jdbc:postgresql://localhost:5432/securebank`
- `jwt.secret`: shared with `auth-service` — tokens minted there validate here.
- `accounts-service.base-url`: `http://localhost:8084` (see limitation above)
- `audit-service.base-url` / `audit-service.api-key`: `http://127.0.0.1:8089` / matches `security/audit-recovery-service`'s default `AUDIT_SERVICE_API_KEY`.
- `payments.receipt-secret`: seed for the SHA-256 receipt reference generator.
- `payments.kafka.completed-topic` / `payments.kafka.held-topic`: Kafka topic names.

---

## 🧪 Running Automated Tests

Full `MockMvc` integration test suite (`PaymentControllerTest.java`) runs against an H2 in-memory database. `AccountsServiceClient` and `AuditRecoveryClient` are replaced by test-profile fakes (`FakeAccountsServiceClient`, `FakeAuditRecoveryClient`) so the suite needs neither a live `accounts-service` nor a live `audit-recovery-service`:

```bash
# From monorepo root
mvn clean test -pl backend/payments-service

# Or from payments-service directory
mvn clean test
```

---

## 🚀 How to Run Service Locally

### 1. Start PostgreSQL and Kafka

```bash
docker compose up -d postgres kafka
```

### 2. (Optional but needed for real payments) Start audit-recovery-service and accounts-service

```bash
cd security/audit-recovery-service && cargo run   # fraud hook — real, already built
# accounts-service is not built yet — /pay and /qr/pay will 503 until it exists
```

### 3. Launch Spring Boot Application

```bash
mvn spring-boot:run
```

The service will start on port `8086`.
