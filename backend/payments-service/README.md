# Payments Service (`payments-service`)

The **Payments Service** processes Account-to-Vendor (A2V) external payments, merchant settlements, and QR code transaction flows.

---

## 🎯 What to Develop

- **External Merchant Payments (A2V)**: Settle transactions between customers and registered merchants (`FR-15`).
- **QR-Based Payments**: Decode and process QR merchant code payments (`FR-20`).
- **Digital Receipts**: Generate cryptographically reference-numbered digital transaction receipts (`FR-21`).
- **Fraud Detection Hook**: Intercept suspicious high-velocity vendor payments (`FR-31`).

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

The service will start on port `8086`.
