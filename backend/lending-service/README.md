# Lending Service (`lending-service`)

The **Lending Service** handles loan originations, application status tracking, repayment schedules, and automated installment deductions.

---

## 🎯 What to Develop

- **Loan Applications**: Application submission workflow for personal and SME loans (`FR-22`).
- **Real-Time Status Tracking**: Application status transitions (`Submitted`, `Under Review`, `Approved`, `Disbursed`, `Rejected`) (`FR-23`).
- **Repayment Schedules**: Calculate and display interest, principal breakdown, installment due dates (`FR-24`).
- **Automated Repayment**: Auto-deduct loan repayments from linked customer current/savings accounts with retry policies (`FR-25`).
- **Repayment Reminders**: Event triggers sent to Notification Service (`FR-26`).

---

## 🛠️ Prerequisites

- JDK 21 LTS
- Apache Maven 3.9+
- PostgreSQL 16 (`localhost:5432`)

---

## 🚀 How to Setup & Run

```bash
docker compose up -d postgres
mvn clean compile
mvn spring-boot:run
```

The service will start on port `8087`.
