# Accounts & Ledger Service (`accounts-service`)

The **Accounts Service** owns core banking account management, real-time ledger balance tracking, transaction history, and account freeze capabilities.

---

## 🎯 What to Develop

- **Digital Account Creation**: Create savings and current accounts for verified users.
- **Account Dashboard & Balances**: Display real-time account status and balance information.
- **Transaction Ledger**: Queryable, searchable transaction history by date range, amount, and type.
- **Statement Generation**: Download monthly PDF statements.
- **Self-Service Account Freeze**: Allow instant account freezing/unfreezing by account owners.
- **Event Streaming**: Publish ledger state changes to Apache Kafka for immutable audit logging.

---

## 🛠️ Prerequisites

- JDK 21 LTS
- Apache Maven 3.9+
- PostgreSQL 16 (`localhost:5432`)
- Apache Kafka (`localhost:9092`)

---

## 🚀 How to Setup & Run

```bash
docker compose up -d postgres kafka
mvn clean compile
mvn spring-boot:run
```

The service will start on port `8084`.

---

## No seeded data

Nothing is pre-populated. A customer who has just registered owns no accounts, no cards and no
transactions, and every endpoint answers from `accounts`, `bank_cards` and `account_transactions`
scoped to the caller the gateway authenticated (`X-User-Id`). A portfolio only appears once the
customer opens an account, or claims one the bank has already issued.

**Linking an existing account** matches `accounts.account_number` + `accounts.holder_national_id`
on a row whose `user_id` is still null (issued by the bank, unclaimed). **Linking a credit card**
does the same against `bank_cards`. On a fresh database there are no such rows, so both correctly
report that nothing matched. To try the flow locally, insert an unclaimed row yourself.

Confirmation always uses the current six-digit code from the customer's authenticator app, checked
by `totp-service`.

---

## Ledger routes (`/internal`)

Money is moved by other core services through routes the API gateway does **not** publish, so no
browser can reach them:

| Route                                                  | Used by                                         |
| ------------------------------------------------------ | ----------------------------------------------- |
| `GET /internal/v1/accounts/{id}`                       | transfer, lending - balance before moving money |
| `POST /internal/v1/accounts/{id}/debit`                | transfer - takes money off the sender           |
| `POST /internal/v1/accounts/{id}/credit`               | credits a known account                         |
| `POST /internal/v1/accounts/by-user/{userId}/debit`    | payments - charges the payer's primary account  |
| `POST /internal/v1/accounts/by-number/{number}/credit` | transfer - pays an in-bank beneficiary          |
| `GET /internal/v1/accounts/journal`                    | notification-service - the audit view's journal |

Every posting carries a `reference` from the calling service. Re-posting the same reference on the
same account is a no-op, so a retry after a timeout cannot move the money twice.
