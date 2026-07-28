# TOTP MFA Service (`totp-service`)

The **TOTP Service** is a dedicated microservice responsible for generating, rendering QR codes, and cryptographically verifying Time-Based One-Time Passwords (**RFC 6238**) and single-use emergency recovery scratch codes for multi-factor authentication (**FR-04**).

---

## 🎯 Implemented Features & SRS Mapping

- **Base32 Secret Generation**: Generates 160-bit cryptographically random Base32 secret keys per user.
- **Authenticator App QR Codes**: Produces `otpauth://totp/SecureBank:username?secret=...` URIs and Base64 PNG image payloads for scanning into Google Authenticator, Authy, or 1Password.
- **RFC 6238 TOTP Validation**: Validates 6-digit TOTP codes over 30-second time steps with ±1 window (30-second drift) for clock skew tolerance.
- **Single-Use Scratch Recovery Codes**: Generates 8 single-use emergency backup codes during 2FA setup. Verifying a valid scratch code automatically consumes it.

---

## 📡 REST API Endpoint Specification

All endpoints are exposed under `/api/v1/totp`:

| Method | Endpoint                        | Description                                                                     |
| :----- | :------------------------------ | :------------------------------------------------------------------------------ |
| `POST` | `/api/v1/totp/setup/{userId}`   | Initiate 2FA enrollment (generates secret, QR code image, and 8 scratch codes). |
| `POST` | `/api/v1/totp/enable`           | Verify initial 6-digit code to activate 2FA (`enabled: true`).                  |
| `POST` | `/api/v1/totp/verify`           | Validate a 6-digit TOTP code or 8-character single-use scratch code.            |
| `POST` | `/api/v1/totp/disable/{userId}` | Disable 2FA with verification code.                                             |
| `GET`  | `/api/v1/totp/status/{userId}`  | Get 2FA enrollment and enabled status for a user.                               |
| `GET`  | `/api/v1/totp/qr/{userId}`      | Get raw PNG byte stream of the QR code image.                                   |

---

## ⚙️ Environment Configuration (`application.yml`)

- `server.port`: `${SERVER_PORT:8082}`
- `totp.issuer`: `${TOTP_ISSUER:SecureBank}`
- `spring.datasource.url`: `${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/securebank}`

---

## 🧪 Running Automated Tests

Run the full integration test suite (`TotpControllerTest.java`) using H2 in-memory test database:

```bash
mvn clean test -pl backend/totp-service
```
