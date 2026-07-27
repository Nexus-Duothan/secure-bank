# Transfer Service (`transfer-service`)

The **Transfer Service** orchestrates internal Account-to-Account (A2A) fund transfers between platform accounts.

---

## 🎯 What to Develop

- **Internal Fund Transfers (A2A)**: Execute real-time transfers with balance sufficiency checks (`FR-14`).
- **Payee Management**: Add/edit payees with a 12-hour cooling period for large transfers (`FR-16`).
- **Transfer Confirmation**: Summarize transfer details (recipient, fee, amount) prior to execution (`FR-17`).
- **Transaction Limits**: Enforce daily and per-transaction upper bounds (`FR-18`).
- **Scheduled Payments**: One-time future & recurring payment scheduling (`FR-19`).
- **ACID Compliance & Idempotency**: Guarantee atomic ledger balance updates (`NFR-R2`).

---

## 🛠️ Prerequisites

- JDK 21 LTS
- Apache Maven 3.9+
- PostgreSQL 16 (`localhost:5432`)
- Apache Kafka (`localhost:9092`)

---

## 🚀 How to Setup & Run

```bash
docker compose up -d postgres kafka
mvn clean compile
mvn spring-boot:run
```

The service will start on port `8085`.
