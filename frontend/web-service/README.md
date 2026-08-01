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

---

## 🔌 Talking to the Backend

The client never calls a service port directly — it requests relative `/api/...` paths so traffic goes through the API Gateway, the platform's single hardened entry point (blueprint §2.2). Copy `.env.example` to `.env.local` to change where that lands:

| Variable                | Default                 | Purpose                                        |
| :---------------------- | :---------------------- | :--------------------------------------------- |
| `VITE_API_PROXY_TARGET` | `http://localhost:8080` | Where the dev server forwards `/api`           |
| `VITE_USER_API_BASE`    | `/api/v1/users`         | Absolute override to bypass the proxy entirely |

To work against the user-service alone, without the gateway running:

```bash
VITE_API_PROXY_TARGET=http://localhost:8083 npm run dev
```

If the backend is unreachable the app falls back to a local preview of the demo profile and says so in a banner — changes made in that mode are **not** saved. Confirm them with `492000`.

While the user-service runs with `securebank.user.otp.expose-code` enabled it returns the generated code in the challenge response, and the OTP screen displays it. With that disabled the code arrives through the Notification Service instead and the screen shows no hint.

The **Admin / RBAC** screen stands in for a signed-in staff session: it loads the directory with an administrator role header and acts as the seeded administrator, so the service's own authorisation rules (officers cannot grant roles, nobody may re-grade themselves) are exercised for real and their rejections surface in the UI.
