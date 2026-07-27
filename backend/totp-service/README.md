# TOTP MFA Service (`totp-service`)

The **TOTP Service** provides time-based one-time password generation and validation for multi-factor authentication (MFA).

---

## 🎯 What to Develop

- **TOTP Secret Generation**: Securely generate base32 TOTP secrets per user account.
- **QR Code Payload**: Produce standard `otpauth://` URI endpoints for authentication apps (Google Authenticator, Authy).
- **OTP Validation**: Validate 6-digit TOTP tokens with a rolling time step (30 seconds) (`FR-04`).
- **High-Risk Action Step-Up**: Provide verification hooks for adding payees, large transfers, and password changes.

---

## 🛠️ Prerequisites

- JDK 21 LTS
- Apache Maven 3.9+

---

## 🚀 How to Setup & Run

### 1. Build & Run

```bash
mvn clean compile
mvn spring-boot:run
```

The service will start on port `8082`.
