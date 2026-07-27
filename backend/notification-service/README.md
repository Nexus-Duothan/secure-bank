# Notification Service (`notification-service`)

The **Notification Service** acts as an event-driven multi-channel communication engine delivering SMS, email, and push notifications.

---

## 🎯 What to Develop

- **Kafka Event Consumer**: Listen to transaction and security events from `accounts-service`, `transfer-service`, and `auth-service`.
- **Real-Time Transaction Alerts**: Instantly alert users on account debits, credits, logins (`FR-27`).
- **Security Alerts**: Immediate security notifications for failed logins, account holds, or new payees (`FR-28`).
- **Multi-Channel Delivery**: Support Email, Push, and SMS with guaranteed SMS fallback for critical security alerts (`FR-29`).

---

## 🛠️ Prerequisites

- JDK 21 LTS
- Apache Maven 3.9+
- Apache Kafka (`localhost:9092`)

---

## 🚀 How to Setup & Run

```bash
docker compose up -d kafka
mvn clean compile
mvn spring-boot:run
```

The service will start on port `8088`.
