# Authentication Service (`auth-service`)

The **Authentication Service** is the central identity authority, credential store, and session manager for the SecureBank platform. Built on Spring Boot 3.3.2 and JDK 21 LTS, it implements two-step authentication, TOTP MFA verification, manual Bank Officer KYC review workflows, active device session tracking, and password recovery.

---

## 🎯 Implemented Features & SRS Mapping

- **User Registration & State Machine** (`FR-01`, `FR-02`): Customer registration (`PENDING_KYC`), document submission (`UNDER_REVIEW`), and officer review/approval queue (`ACTIVE` / `REJECTED`).
- **Credential Hashing & Protection** (`FR-03`, `NFR-S4`): Salted password storage using Spring Security's `BCryptPasswordEncoder` (cost factor 12).
- **Two-Step Login & TOTP MFA** (`FR-04`): Step 1 verifies credentials and returns a pre-auth token; Step 2 verifies 6-digit TOTP codes before issuing JWT sessions.
- **Session Management & Device Revocation** (`FR-05`): JJWT 0.12.6 HMAC-SHA256 tokens (5-minute short-lived access tokens, 7-day refresh tokens), IP & User-Agent device parsing, active session listing, and remote session revocation.
- **Password Recovery** (`FR-06`): Token-based, 15-minute expiring, MFA-protected password resets that automatically revoke all active user sessions upon completion.
- **Role-Based Access Control** (`FR-08`, `NFR-S5`): RBAC enforcement for `CUSTOMER`, `MERCHANT`, `BANK_OFFICER`, and `ADMIN` roles.

---

## 📡 REST API Endpoint Specification

All endpoints are exposed under `/api/v1/auth`:

| Method   | Endpoint                               | Authorization            | Description                                                               |
| :------- | :------------------------------------- | :----------------------- | :------------------------------------------------------------------------ |
| `POST`   | `/api/v1/auth/register`                | Public                   | Register new customer (`PENDING_KYC`).                                    |
| `POST`   | `/api/v1/auth/verify-kyc`              | Authenticated            | Submit KYC document payload (`UNDER_REVIEW`).                             |
| `GET`    | `/api/v1/auth/officer/kyc/pending`     | `BANK_OFFICER` / `ADMIN` | Fetch queue of pending KYC applications.                                  |
| `POST`   | `/api/v1/auth/officer/kyc/{id}/review` | `BANK_OFFICER` / `ADMIN` | Manual review: Approve (`ACTIVE`) or Reject (`REJECTED`).                 |
| `POST`   | `/api/v1/auth/login`                   | Public                   | Step 1 login: Verifies credentials, returns `preAuthToken`.               |
| `POST`   | `/api/v1/auth/login/verify-mfa`        | Public                   | Step 2 login: Verifies TOTP code, returns Bearer access & refresh tokens. |
| `POST`   | `/api/v1/auth/refresh`                 | Public                   | Rotates refresh token & issues new access token.                          |
| `GET`    | `/api/v1/auth/sessions`                | Authenticated            | View list of active device sessions.                                      |
| `DELETE` | `/api/v1/auth/sessions/{id}`           | Authenticated            | Revoke a specific active device session.                                  |
| `POST`   | `/api/v1/auth/password-reset/request`  | Public                   | Request password reset token via email.                                   |
| `POST`   | `/api/v1/auth/password-reset/confirm`  | Public                   | Complete password reset with token & TOTP code.                           |
| `GET`    | `/api/v1/auth/validate`                | Internal / Gateway       | Validate JWT token signature, claims, and role permissions.               |

---

## ⚙️ Environment Configuration (`application.yml`)

- `server.port`: `8081`
- `spring.datasource.url`: `jdbc:postgresql://localhost:5432/securebank`
- `jwt.secret`: Base64-encoded HMAC-SHA256 secret key.
- `jwt.access-token-expiration-seconds`: `300` (5 minutes)
- `jwt.refresh-token-expiration-seconds`: `604800` (7 days)

---

## 🧪 Running Automated Tests

Run the full integration test suite (`AuthControllerTest.java`) using H2 in-memory test database:

```bash
# From monorepo root
mvn clean test -pl backend/auth-service

# Or from auth-service directory
mvn clean test
```

---

## 🚀 How to Run Service Locally

### 1. Start PostgreSQL

```bash
docker compose up -d postgres
```

### 2. Launch Spring Boot Application

```bash
mvn spring-boot:run
```

The service will start on port `8081`.
