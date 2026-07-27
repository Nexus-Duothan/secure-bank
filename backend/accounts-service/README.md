# Accounts & Ledger Service (`accounts-service`)

The **Accounts Service** owns core banking account management, real-time ledger balance tracking, transaction history, and account freeze capabilities.

---

## 🎯 What to Develop

- **Digital Account Creation**: Create savings and current accounts for verified users (`FR-09`).
- **Account Dashboard & Balances**: Display real-time account status and balance information (`FR-10`).
- **Transaction Ledger**: Queryable, searchable transaction history by date range, amount, and type (`FR-11`).
- **Statement Generation**: Download monthly PDF statements (`FR-12`).
- **Self-Service Account Freeze**: Allow instant account freezing/unfreezing by account owners (`FR-13`).
- **Event Streaming**: Publish ledger state changes to Apache Kafka for immutable audit logging.

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

The service will start on port `8084`.
