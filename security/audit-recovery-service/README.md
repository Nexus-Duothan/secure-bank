# Audit & Recovery Service (`audit-recovery-service`)

The **Audit & Recovery Service** is a memory-safe **Rust** microservice responsible for maintaining an immutable, append-only, cryptographic SHA-256 tamper-evident transaction journal (**FR-30**), detecting anomalous behavior (**FR-31**), and enabling automated microservice state recovery through clean audit log replay (**FR-32**, **NFR-D5**).

---

## 🎯 Implemented Features & SRS Mapping

- **Cryptographic SHA-256 Hash Chaining**: Every audit entry is cryptographically linked to the preceding entry using SHA-256 Merkle hash chaining (`hash = SHA256(prev_hash + id + timestamp + service_name + event_type + payload)`).
- **Tamper-Evident Integrity Verification**: Computes sequential cryptographic digests over the journal to detect modified payloads or out-of-order deletion.
- **Microservice State Replay & Recovery**: Streams verified historical event logs per service scope to automatically reconstruct state following database corruption or disaster recovery (**FR-32**, **NFR-D5**, **NFR-D1**).
- **Fraud & Anomaly Detection**: Analyzes event frequency and authentication failures to flag high-risk operations and issue account holds (**FR-31**).

---

## 📡 REST API Endpoint Specification

All endpoints are exposed under `/api/v1/audit`:

| Method | Endpoint                              | Description                                                                                  |
| :----- | :------------------------------------ | :------------------------------------------------------------------------------------------- |
| `POST` | `/api/v1/audit/entries`               | Record a new audit event (computes SHA-256 hash chain and appends to journal).               |
| `GET`  | `/api/v1/audit/entries`               | Query audit trail with filtering by `service_name`, `user_id`, or `event_type`.              |
| `GET`  | `/api/v1/audit/verify`                | Perform full cryptographic integrity check over the audit journal chain.                     |
| `POST` | `/api/v1/audit/replay/{service_name}` | Replay clean, verified event logs for rebuilding microservice state (**FR-32**).             |
| `GET`  | `/api/v1/audit/anomalies`             | Query active anomaly and threat detection reports for administrative monitoring (**FR-34**). |

---

## 🚀 How to Setup & Run

### 1. Build & Run Service

```bash
cd security/audit-recovery-service
cargo check
cargo run
```

The service will start an Axum HTTP REST server on port `8089`.

---

## 🧪 Running Automated Tests

Run the integration test suite (`tests/audit_tests.rs`):

```bash
cd security/audit-recovery-service
cargo test
```
