# SecureBank - Resilient Cloud-Native Digital Banking Platform

> **Project Blueprint & Software Requirements Specification Baseline**: [docs/proposal.md](docs/proposal.md)

SecureBank is a cloud-native, microservices-based digital banking platform designed to safely restore financial access (core banking, payments, transfers, and lending) following systemic cyber disruptions. By replacing monolithic architectures with isolated microservices and incorporating an immutable Rust audit journal, any service compromise is contained without risking total system collapse.

---

## 🏗️ Repository Architecture

The project is structured as a polyglot monorepo:

```
secure-bank/
├── docs/
│   └── proposal.md                      # Blueprint & SRS document
├── docker-compose.yml                   # Infrastructure (PostgreSQL, Kafka, ELK)
├── pom.xml                              # Root Maven Parent POM for Java microservices
├── package.json                         # Monorepo DX & Git pre-commit hooks
├── backend/                             # Java Spring Boot Microservices (JDK 21)
│   ├── api-gateway                      # Unified Entry Point & Routing (Port 8080)
│   ├── auth-service                     # Authentication & Token Management (Port 8081)
│   ├── totp-service                     # MFA TOTP Generation & Verification (Port 8082)
│   ├── user-service                     # User Profiles & RBAC (Port 8083)
│   ├── accounts-service                 # Core Ledger & Account Balances (Port 8084)
│   ├── transfer-service                 # Internal Account-to-Account Transfers (Port 8085)
│   ├── payments-service                 # External Account-to-Vendor Payments (Port 8086)
│   ├── lending-service                  # Loan Origination & Repayments (Port 8087)
│   └── notification-service             # Multi-Channel Alerts (Port 8088)
├── security/
│   └── audit-recovery-service           # Rust Immutable Journaling & Recovery Engine
    └── frontend/
        └── web-service                      # React 18 + Vite + Ant Design Web App (Port 5173)
```

---

## 🛠️ Prerequisites

Ensure the following tools are installed before setting up the workspace:

- **Java Development Kit**: JDK 21 LTS (managed via SDKMAN or system package)
- **Apache Maven**: 3.9+
- **Rust Toolchain**: `cargo` and `rustc` 1.97+
- **Node.js & npm**: Node.js 24+ and npm 11+
- **Docker & Docker Compose**: Docker Engine 24+ & Docker Compose 2+
- **Git**: 2.40+

---

## 🚀 Quick Start Setup

### 1. Clone & Initialize Git Hooks

```bash
git clone <repository-url> secure-bank
cd secure-bank

# Install root DX tools & Git pre-commit hooks
npm install
npx husky init
```

### 2. Start Infrastructure Services

Launch PostgreSQL 16, Apache Kafka, and the ELK stack:

```bash
docker compose up -d
```

### 3. Build Backend Microservices (Java)

```bash
mvn clean compile
```

### 4. Build Security Service (Rust)

```bash
cd security/audit-recovery-service
cargo check
cd ../..
```

### 5. Install & Launch Web Service (React Frontend)

```bash
cd frontend/web-service
npm install
npm run dev
```

The web application will be accessible at `http://localhost:5173`.

---

## 📡 Service Port Allocation Table

| Service Layer      | Module Directory                  | Technology                  | Port   | Primary Endpoint                             |
| :----------------- | :-------------------------------- | :-------------------------- | :----- | :------------------------------------------- |
| **Edge Gateway**   | `backend/api-gateway`             | Java (Spring Cloud Gateway) | `8080` | `http://localhost:8080`                      |
| **Authentication** | `backend/auth-service`            | Java (Spring Boot)          | `8081` | `http://localhost:8081/api/v1/auth`          |
| **MFA / TOTP**     | `backend/totp-service`            | Java (Spring Boot)          | `8082` | `http://localhost:8082/api/v1/totp`          |
| **User & RBAC**    | `backend/user-service`            | Java (Spring Boot)          | `8083` | `http://localhost:8083/api/v1/users`         |
| **Accounts**       | `backend/accounts-service`        | Java (Spring Boot)          | `8084` | `http://localhost:8084/api/v1/accounts`      |
| **Transfers**      | `backend/transfer-service`        | Java (Spring Boot)          | `8085` | `http://localhost:8085/api/v1/transfers`     |
| **Payments**       | `backend/payments-service`        | Java (Spring Boot)          | `8086` | `http://localhost:8086/api/v1/payments`      |
| **Lending**        | `backend/lending-service`         | Java (Spring Boot)          | `8087` | `http://localhost:8087/api/v1/loans`         |
| **Notification**   | `backend/notification-service`    | Java (Spring Boot)          | `8088` | `http://localhost:8088/api/v1/notifications` |
| **Audit Engine**   | `security/audit-recovery-service` | Rust                        | -      | Event Consumer / gRPC                        |
| **Web Service**    | `frontend/web-service`            | React + Vite + Ant Design   | `5173` | `http://localhost:5173`                      |

---

## 🎨 Code Formatting & Developer Experience (DX)

Automatic code formatting triggers on every `git commit` via `husky` and `lint-staged`:

- **JS, TS, TSX, JSON, CSS, MD, YML, Java**: Formatted automatically via Prettier (`prettier-plugin-java`).
- **Rust**: Formatted automatically via `cargo fmt`.

To run formatting checks manually across the monorepo:

```bash
npm run format         # Auto-format all supported files
npm run format:check   # Validate formatting without modifying
```

## Local development

Use the API Gateway as the single entry point in local development.

Ports:

- `5173` frontend
- `8080` API Gateway
- `8081` auth-service
- `8082` totp-service
- `8083` user-service
- `8084` accounts-service
- `8085` transfer-service
- `8086` payments-service
- `8087` lending-service
- `8088` notification-service

Quick start on Windows PowerShell:

```powershell
cd C:\Users\knimn\OneDrive\Documents\GitHub\securebank\secure-bank
docker start securebank-postgres
mvn -pl backend/auth-service spring-boot:run
```

Open another terminal for each service:

```powershell
cd C:\Users\knimn\OneDrive\Documents\GitHub\securebank\secure-bank
mvn -pl backend/totp-service spring-boot:run
mvn -pl backend/user-service spring-boot:run
mvn -pl backend/accounts-service spring-boot:run
mvn -pl backend/transfer-service spring-boot:run
mvn -pl backend/payments-service spring-boot:run
mvn -pl backend/lending-service spring-boot:run
mvn -pl backend/notification-service spring-boot:run
mvn -pl backend/api-gateway spring-boot:run
```

Frontend:

```powershell
cd C:\Users\knimn\OneDrive\Documents\GitHub\securebank\secure-bank\frontend\web-service
npm run dev
```

Or use the helper script:

```powershell
cd C:\Users\knimn\OneDrive\Documents\GitHub\securebank\secure-bank
.\scripts\start-dev.ps1
```

Helpful dev commands:

```powershell
cd C:\Users\knimn\OneDrive\Documents\GitHub\securebank\secure-bank\frontend\web-service
npm test

cd C:\Users\knimn\OneDrive\Documents\GitHub\securebank\secure-bank
npx lint-staged
```

`lint-staged` is configured at the monorepo root, so run it from `secure-bank`, not from `frontend/web-service`.

Demo login:

- Username: `kaveesha.demo`
- Password: `SecureBank@123`
- OTP: any 6 digits, for example `123456`
