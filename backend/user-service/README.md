# User & RBAC Service (`user-service`)

The **User Service** manages customer profile data, contact preferences, linked devices, and Role-Based Access Control (RBAC) permissions.

---

## 🎯 What to Develop

- **Profile Management**: CRUD operation endpoints for updating user profiles and notification channels (`FR-07`).
- **Role-Based Access Control (RBAC)**: Manage granular roles (Customer, Merchant, Bank Officer, Administrator) (`FR-08`).
- **Device Linking**: Maintain records of authorized user devices.

---

## 🛠️ Prerequisites

- JDK 21 LTS
- Apache Maven 3.9+
- PostgreSQL 16 (`localhost:5432`)

---

## 🚀 How to Setup & Run

```bash
docker compose up -d postgres
mvn clean compile
mvn spring-boot:run
```

The service will start on port `8083`.
