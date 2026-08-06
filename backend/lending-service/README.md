# Lending Service (`lending-service`)

The **Lending Service** handles loan origination, application review, repayment schedules, automated installment collection, and repayment reminders.

---

## What's implemented

- **Loan applications** (`FR-22`): `POST /api/v1/loans/apply` validates the requested amount/term against configured platform bounds and creates an application in `UNDER_REVIEW`.
- **Status tracking** (`FR-23`): `GET /api/v1/loans/applications`, `GET /api/v1/loans/applications/{id}` for the applicant; `GET /api/v1/loans/officer/pending` + `POST /api/v1/loans/officer/{id}/review` (role-gated to `BANK_OFFICER`/`ADMIN`) for review. Approving an application disburses it in the same transaction — creates the `Loan`, computes and persists its full amortization schedule, and publishes a `loans.disbursed.v1` event.
- **Repayment schedule** (`FR-24`): `GET /api/v1/loans/{id}` (summary: remaining balance, installments paid/total, next due date/amount) and `GET /api/v1/loans/{id}/installments` (full schedule). The schedule is a standard reducing-balance (EMI) amortization, computed once at disbursement and persisted per-installment rather than recomputed on the fly.
- **Automated repayment + retry** (`FR-25`): `ScheduledInstallmentRunner` polls every minute for due/retry-due installments and hands each off to `InstallmentExecutionService` for a locked, transactional collection attempt. **Retry policy** (not specified by the FR — a documented judgment call): on insufficient funds (or accounts-service being unreachable, treated the same way), retry once per day for up to 3 attempts before marking the installment `OVERDUE` and the loan `DELINQUENT`. Configurable via `securebank.lending.repayment.*`. `POST /api/v1/loans/{id}/pay` runs the same collection logic on demand (manual early payment), and `PATCH /api/v1/loans/{id}/autopay` lets the borrower opt a loan out of the scheduled runner (manual payment still works either way).
- **Repayment reminders** (`FR-26`): a second scheduled poll publishes a `loans.repayment-reminder.v1` event once per installment, a configurable lead time (default 72h) before its due date. Like the disbursement and overdue events, this is publish-only — notification-service doesn't consume these topics yet, so this is groundwork for when it does.

## Accounts-service limitation

`accounts-service` owns the ledger and `AccountsClient` (`client/AccountsClient.java`) reads real balances from it (`GET /internal/v1/accounts/{id}`). Lending still only **reads**: disbursements and installment collections are recorded on the loan, not posted to the customer's account, so a balance check here is advisory rather than a reservation. Posting them is a matter of calling the debit/credit routes accounts-service now exposes; only `AccountsRestClient` needs to change.

## Testing

- `mvn test` runs both plain Mockito unit tests (`AmortizationCalculatorTest`, `InstallmentExecutionServiceTest`) and a full `@SpringBootTest` + MockMvc suite (`LoanControllerTest`) against H2 (`MODE=PostgreSQL`) with Flyway migrations applied for real. `FakeAccountsClient` stands in for accounts-service so the suite needs neither that service nor a live database running.

---

## Prerequisites

- JDK 21 LTS
- Apache Maven 3.9+
- PostgreSQL 16 (`localhost:5432`)

---

## How to Setup & Run

```bash
docker compose up -d postgres
mvn clean compile
mvn spring-boot:run
```

The service will start on port `8087`.
