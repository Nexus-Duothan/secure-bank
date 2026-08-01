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

If the backend is unreachable, the app falls back to a local preview of the demo profile and shows a banner. Changes made in that mode are not saved.

If `SECUREBANK_USER_OTP_EXPOSE_CODE=true` is enabled on `user-service`, the generated OTP is returned in the challenge response and shown on the OTP screen for local browser-only demos. With the safer default, the code must arrive through the notification flow instead.

The **Admin / RBAC** screen stands in for a signed-in staff session: it loads the directory as the seeded administrator so the backend authorisation rules are exercised for real.
