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

On first start two demo profiles are seeded — a customer (`john.doe@securebank.lk`) and an administrator (`nimali.perera@securebank.lk`) — so the RBAC screens have both a subject and an operator. Disable with `securebank.user.seed-demo-data: false`.

---

## 🔐 Caller Identity

Every endpoint acts on the caller resolved from the identity headers that the API Gateway stamps on proxied requests:

| Header        | Meaning                                         |
| :------------ | :---------------------------------------------- |
| `X-User-Id`   | UUID of the authenticated profile               |
| `X-User-Role` | `CUSTOMER`, `MERCHANT`, `BANK_OFFICER`, `ADMIN` |

These are trusted only because the gateway is the sole ingress: it authenticates the session, strips any client-supplied copies, and re-stamps them over mTLS (`NFR-S3`). `CallerIdentityArgumentResolver` is the seam where gateway-issued JWT verification will replace the raw headers.

While `securebank.user.security.allow-unauthenticated-demo-caller` is `true`, a request with no `X-User-Id` resolves to the seeded demo customer so the web prototype works before auth-service issues tokens. **Set it to `false` outside local development.**

---

## 📖 API

Self-service routes (`/api/v1/users`). Every mutation is staged and takes effect only after OTP confirmation (`FR-07`):

| Method | Path                                    | Purpose                               |
| :----- | :-------------------------------------- | :------------------------------------ |
| `GET`  | `/me`                                   | Current profile, preferences, devices |
| `POST` | `/me/profile-change`                    | Stage contact detail changes          |
| `POST` | `/me/notification-preferences-change`   | Stage channel preference changes      |
| `POST` | `/me/devices/link`                      | Stage a new linked device             |
| `POST` | `/me/devices/trust`                     | Stage marking a device trusted        |
| `POST` | `/me/devices/revoke`                    | Stage revoking a device               |
| `POST` | `/me/freeze`                            | Stage an account freeze (`FR-13`)     |
| `POST` | `/me/unfreeze`                          | Stage an account unfreeze (`FR-13`)   |
| `POST` | `/me/changes/{changeRequestId}/confirm` | Confirm a staged change with the code |

Administration routes (`/api/v1/users/admin`), enforced in the service layer:

| Method  | Path               | Required role             |
| :------ | :----------------- | :------------------------ |
| `GET`   | `/`                | `ADMIN` or `BANK_OFFICER` |
| `GET`   | `/{userId}`        | `ADMIN` or `BANK_OFFICER` |
| `PATCH` | `/{userId}/status` | `ADMIN` or `BANK_OFFICER` |
| `PATCH` | `/{userId}/role`   | `ADMIN` only              |

Granting roles is administrator-only so an officer cannot mint administrators, and no caller may change their own role (`NFR-S5`).

---

## 🔑 One-Time Codes

Each challenge issues its own `SecureRandom` six digit code. Only a BCrypt digest is persisted (`NFR-S4`), verification is constant-time, and a challenge is burned after `securebank.user.otp.max-attempts` wrong codes so the six digit space cannot be walked (`FR-33`). Unconfirmed challenges are purged once they expire.

Delivery over the customer's preferred channel belongs to the Notification Service (`FR-29`). Until that hop exists, `securebank.user.otp.expose-code: true` echoes the code back in the challenge response for local demos — **set it to `false` outside local development.**

---

## ⚙️ Configuration

| Property                                                     | Default               | Purpose                                       |
| :----------------------------------------------------------- | :-------------------- | :-------------------------------------------- |
| `securebank.user.seed-demo-data`                             | `true`                | Seed the demo customer and administrator      |
| `securebank.user.otp.ttl`                                    | `PT5M`                | Challenge lifetime                            |
| `securebank.user.otp.max-attempts`                           | `5`                   | Wrong codes before a challenge is burned      |
| `securebank.user.otp.expose-code`                            | `true`                | Return the code in the response (local only)  |
| `securebank.user.security.allow-unauthenticated-demo-caller` | `true`                | Resolve header-less requests to the demo user |
| `securebank.user.cors.allowed-origins`                       | `localhost:3000/5173` | Direct browser access during development      |

Database credentials come from `SECUREBANK_DB_URL`, `SECUREBANK_DB_USER`, and `SECUREBANK_DB_PASSWORD`.

---

## 🧪 Tests

```bash
mvn -pl backend/user-service test
```

Covers the OTP lifecycle (hashing, attempt limiting, expiry, cross-user scoping), freeze/unfreeze, and the RBAC rules.
