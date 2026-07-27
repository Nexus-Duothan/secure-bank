# API Gateway Service (`api-gateway`)

The **API Gateway** acts as the single entry point for all incoming external requests to the SecureBank platform. Built with Spring Cloud Gateway on JDK 21, it enforces SSL/TLS termination, request routing, rate limiting, and baseline authentication.

---

## 🎯 What to Develop

- **Request Routing**: Proxy rules for all internal backend services (`/api/v1/auth`, `/api/v1/users`, `/api/v1/accounts`, `/api/v1/transfers`, `/api/v1/payments`, `/api/v1/loans`).
- **Rate Limiting**: Redis/In-memory token bucket rate limiting per IP and authenticated user session (`FR-33`).
- **Zero-Trust Token Validation**: Verify JWT session tokens before forwarding traffic downstream to internal microservices (`NFR-S3`).
- **TLS Termination**: Enforce TLS 1.3 for incoming client traffic (`NFR-S1`).

---

## 🛠️ Prerequisites

- JDK 21 LTS
- Apache Maven 3.9+

---

## 🚀 How to Setup & Run

### 1. Build

```bash
# From module directory
mvn clean compile

# Or from monorepo root
mvn clean compile -pl backend/api-gateway
```

### 2. Run Locally

```bash
mvn spring-boot:run
```

The service will start on port `8080`.

---

## ⚙️ Configuration (`application.yml`)

- `server.port`: `8080`
- Configured routes:
  - `auth-service`: `http://localhost:8081`
  - `user-service`: `http://localhost:8083`
  - `accounts-service`: `http://localhost:8084`
  - `transfer-service`: `http://localhost:8085`
  - `payments-service`: `http://localhost:8086`
  - `lending-service`: `http://localhost:8087`
