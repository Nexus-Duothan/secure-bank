# Authentication Service (`auth-service`)

The **Authentication Service** manages user credentials, secure sign-in, session token issuance, and password recovery for the SecureBank platform.

---

## 🎯 What to Develop

- **User Registration**: Register new/returning customers against restored backup data (`FR-01`).
- **Identity Verification (KYC)**: National ID / passport document validation (`FR-02`).
- **Secure Sign-in**: Authenticate users using salted password hashing (Argon2id/bcrypt) (`FR-03`, `NFR-S4`).
- **Session Token Management**: Issue short-lived JWT tokens, enforce 5-minute inactivity timeouts, and manage active device sessions (`FR-05`).
- **Password Recovery**: MFA-protected password reset flows (`FR-06`).

---

## 🛠️ Prerequisites

- JDK 21 LTS
- Apache Maven 3.9+
- PostgreSQL 16 (running on `localhost:5432` via `docker compose up -d`)

---

## 🚀 How to Setup & Run

### 1. Start Database

```bash
docker compose up -d postgres
```

### 2. Build & Run

```bash
mvn clean compile
mvn spring-boot:run
```

The service will start on port `8081`.
