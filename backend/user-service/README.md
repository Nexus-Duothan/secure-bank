# User & RBAC Service (`user-service`)

The **User Service** manages customer profile data, contact preferences, linked devices, staged OTP-confirmed account changes, and role-based administration.

---

## What It Covers

- Profile management
- Notification preference updates
- Linked device management
- RBAC directory operations for staff

---

## Prerequisites

- JDK 21 LTS
- Apache Maven 3.9+
- PostgreSQL 16 on `localhost:5432`

---

## Run Locally

```bash
docker compose up -d postgres
mvn -pl backend/user-service spring-boot:run
```

The service starts on port `8083`.

Local walkthrough accounts are now expected to exist in the development database already rather than being inserted at startup.

---

## Caller Identity

Every endpoint resolves the caller from the bearer access token.

- `user-service` validates the JWT locally using `jwt.secret`
- forwarded `X-User-Id` and `X-User-Role` headers are treated only as consistency checks
- direct header spoofing is rejected

Header-less impersonation of the oldest profile is off by default. Only enable it deliberately for
local prototyping:

```powershell
$env:SECUREBANK_USER_ALLOW_UNAUTHENTICATED_DEMO_CALLER="true"
```

---

## API

Self-service routes live under `/api/v1/users`. Every mutation is staged first and only takes effect after OTP confirmation.

| Method | Path                                    | Purpose                               |
| :----- | :-------------------------------------- | :------------------------------------ |
| `GET`  | `/me`                                   | Current profile, preferences, devices |
| `POST` | `/me/profile-change`                    | Stage contact detail changes          |
| `POST` | `/me/notification-preferences-change`   | Stage notification preference changes |
| `POST` | `/me/devices/link`                      | Stage a new linked device             |
| `POST` | `/me/devices/trust`                     | Stage marking a device trusted        |
| `POST` | `/me/devices/revoke`                    | Stage revoking a device               |
| `POST` | `/me/changes/{changeRequestId}/confirm` | Confirm a staged change with the OTP  |

Administration routes live under `/api/v1/users/admin`.

| Method  | Path               | Required role             |
| :------ | :----------------- | :------------------------ |
| `GET`   | `/`                | `ADMIN` or `BANK_OFFICER` |
| `GET`   | `/{userId}`        | `ADMIN` or `BANK_OFFICER` |
| `PATCH` | `/{userId}/status` | `ADMIN` or `BANK_OFFICER` |
| `PATCH` | `/{userId}/role`   | `ADMIN` only              |

---

## Confirmation codes

Every staged change is confirmed with the current six-digit code from the customer's authenticator
app, checked by `totp-service`. Nothing generates, stores or returns a code here: `demoCode` in the
challenge response is always `null`, and the challenge is burned after too many wrong attempts.

---

## Configuration

| Property                                                     | Default                 | Purpose                                           |
| :----------------------------------------------------------- | :---------------------- | :------------------------------------------------ |
| `securebank.user.otp.ttl`                                    | `PT5M`                  | Challenge lifetime                                |
| `securebank.user.otp.max-attempts`                           | `5`                     | Wrong codes before a challenge is burned          |
| `securebank.notification.service-url`                        | `http://localhost:8088` | Notification service base URL for OTP dispatch    |
| `securebank.user.security.allow-unauthenticated-demo-caller` | `false`                 | Resolve token-less requests to the oldest profile |
| `securebank.user.cors.allowed-origins`                       | `localhost:3000/5173`   | Direct browser access during development          |
| `jwt.secret`                                                 | shared local dev secret | JWT validation secret                             |

Database credentials come from:

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`

---

## Tests

```bash
mvn -pl backend/user-service test
```
