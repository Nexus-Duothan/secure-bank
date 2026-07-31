# Audit & Recovery Service (`audit-recovery-service`)

The **Audit & Recovery Service** is a memory-safe **Rust** microservice responsible for maintaining an immutable, append-only, cryptographic SHA-256 tamper-evident transaction journal (**FR-30**), detecting anomalous behavior (**FR-31**), and enabling automated microservice state recovery through clean audit log replay (**FR-32**, **NFR-D5**).

---

## 🎯 Implemented Features & SRS Mapping

- **Delimited SHA-256 Hash Chaining**: Every audit entry is cryptographically linked using length-prefixed and null-byte delimited SHA-256 hashing (`hash = SHA256(len(prev_hash):prev_hash + ...)`), preventing field collision ambiguities (**FR-30**).
- **Asynchronous Persisted Journal**: Uses non-blocking async `tokio::fs` I/O to guarantee robust disk persistence (`audit_journal.jsonl`) and returns HTTP 500 error status on I/O failures.
- **Internal Service Authentication**: Secured via internal API key header (`X-Internal-Service-Key` or `Authorization: Bearer <token>`) and bound to local interface `127.0.0.1:8089` by default.
- **Verified Microservice State Replay**: Performs mandatory cryptographic integrity validation (`verify_integrity()`) prior to replaying state streams (**FR-32**, **NFR-D5**). Returns `409 CONFLICT` if tampered or corrupted journal logs are detected.
- **Time-Windowed Fraud & Anomaly Engine**: Evaluates rolling time-windowed event bursts (e.g. 3 failed logins within 1 hour) with status tracking (`ACTIVE`/`RESOLVED`) (**FR-31**).

---

## 📡 REST API Endpoint Specification

All endpoints are exposed under `/api/v1/audit` and require `X-Internal-Service-Key` or `Authorization: Bearer <token>`:

| Method | Endpoint                              | Description                                                                                    |
| :----- | :------------------------------------ | :--------------------------------------------------------------------------------------------- |
| `POST` | `/api/v1/audit/entries`               | Record a new audit event (computes SHA-256 hash chain and appends to journal).                 |
| `GET`  | `/api/v1/audit/entries`               | Query audit trail with filtering (`service_name`, `user_id`, `event_type`, `limit`, `offset`). |
| `GET`  | `/api/v1/audit/verify`                | Perform full cryptographic integrity check over the audit journal chain.                       |
| `POST` | `/api/v1/audit/replay/{service_name}` | Replay clean, verified event logs for rebuilding microservice state (**FR-32**).               |
| `GET`  | `/api/v1/audit/anomalies`             | Query active anomaly and threat detection reports for administrative monitoring (**FR-34**).   |

---

## 🚀 How to Setup & Run

### 1. Build & Run Service

```bash
cd security/audit-recovery-service
cargo check
cargo run
```

The service will start an Axum HTTP REST server on `127.0.0.1:8089`.

---

## 🧪 Running Automated Tests

Run the integration test suite (`tests/audit_tests.rs`):

```bash
cd security/audit-recovery-service
cargo test
```
