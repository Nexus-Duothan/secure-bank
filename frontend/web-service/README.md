# Frontend Web Service (`web-service`)

The **Web Service** is the user-facing digital banking client built with React 18, Vite, TypeScript, and Ant Design (`antd`). It translates the wireframes defined in `proposal.md` into an accessible, responsive web application.

---

## 🎯 What to Develop

- **Authentication Screens**: Login, OTP Verification, and Password Reset (`Section 4.1`).
- **Dashboard & Account Details**: Balances, status badges, and transaction history filtering (`Section 4.1`).
- **Transfers & Payments**: Money Transfer (A2A) with confirmation summary, Pay Vendor (A2V) with QR code payment support (`Section 4.1`).
- **Lending Module**: Loan Application submission flow and Repayment Schedule tracker (`Section 4.1`).
- **Security & Audit View**: Real-time transaction audit history, active session revocation, and security alerts.
- **Accessibility & Responsiveness**: Conformance to WCAG 2.1 Level AA and mobile/tablet/desktop layouts (`NFR-U2`, `NFR-U3`).

---

## 🛠️ Prerequisites

- Node.js 24+
- npm 11+

---

## 🚀 How to Setup & Run

### 1. Install Dependencies

```bash
npm install
```

### 2. Launch Development Server

```bash
npm run dev
```

The dev server starts at `http://localhost:3000` with API calls proxied to `http://localhost:8080` (API Gateway).

### 3. Build & Typecheck

```bash
npm run build
npm run lint
```
