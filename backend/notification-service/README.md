# Notification Service (`notification-service`)

The **Notification Service** handles SecureBank outbound OTP and alert delivery.

---

## Implemented Locally

- OTP dispatch endpoint for `user-service`
- SMS-only OTP delivery for SecureBank confirmation codes
- Provider-ready delivery with log-mode fallback and Twilio SMS
- Live notification feed for the frontend notifications screen

---

## Prerequisites

- JDK 21 LTS
- Apache Maven 3.9+
- Optional Twilio credentials for live outbound delivery

---

## How to Setup & Run

```bash
mvn -pl backend/notification-service spring-boot:run
```

The service starts on port `8088`.

---

## Provider Configuration

Default local mode logs outgoing SMS attempts.

### Real SMS via Twilio

```powershell
$env:SECUREBANK_NOTIFICATION_SMS_PROVIDER="twilio"
$env:SECUREBANK_TWILIO_ACCOUNT_SID="<your-account-sid>"
$env:SECUREBANK_TWILIO_AUTH_TOKEN="<your-auth-token>"
$env:SECUREBANK_TWILIO_FROM_NUMBER="+1..."
```

OTP requests are sent only as SMS.
