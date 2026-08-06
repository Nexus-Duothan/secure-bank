# Frontend Web Service (`web-service`)

The **Web Service** is the user-facing digital banking client built with React 18, Vite, TypeScript, and Ant Design (`antd`).

---

## What It Covers

- Authentication screens: login, OTP verification, and password reset
- Dashboard and account details
- Transfers, payments, and lending flows
- Security views such as notifications, profile controls, and linked devices
- Responsive layouts for desktop and mobile

---

## Prerequisites

- Node.js 24+
- npm 11+

---

## Run Locally

### 1. Install dependencies

```bash
npm install
```

### 2. Start the dev server

```bash
npm run dev
```

The dev server starts at `http://localhost:5173`.

### 3. Build and lint

```bash
npm run build
npm run lint
```

---

## Talking To The Backend

The client normally calls relative `/api/...` paths so traffic goes through the API Gateway.

Copy `.env.example` to `.env.local` when you want to change that behavior:

| Variable                | Default                 | Purpose                                            |
| :---------------------- | :---------------------- | :------------------------------------------------- |
| `VITE_API_PROXY_TARGET` | `http://localhost:8080` | Where the Vite dev server forwards `/api` requests |
| `VITE_USER_API_BASE`    | `/api/v1/users`         | Optional absolute override for user-service calls  |

Examples:

```bash
VITE_API_PROXY_TARGET=http://localhost:8080 npm run dev
```

```bash
VITE_USER_API_BASE=http://localhost:8083/api/v1/users npm run dev
```

Every screen shows what the backend returns and nothing else. There is no offline preview and no
sample portfolio: a customer who has just registered sees an empty dashboard with a prompt to open
an account, and balances and activity appear only once something actually posts to the ledger.

Confirmation codes always come from the customer's authenticator app; none is ever returned in an
API response for the UI to display.

The **Admin / RBAC** screen loads the user directory as the signed-in staff member, so the backend
authorisation rules are exercised for real.
