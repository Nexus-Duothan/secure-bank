CREATE TABLE IF NOT EXISTS loan_applications (
  id UUID PRIMARY KEY,
  applicant_user_id UUID NOT NULL,
  purpose VARCHAR(60) NOT NULL,
  amount NUMERIC(19, 2) NOT NULL,
  term_months INTEGER NOT NULL,
  annual_interest_rate NUMERIC(6, 3) NOT NULL,
  linked_account_id VARCHAR(64) NOT NULL,
  status VARCHAR(20) NOT NULL,
  reviewed_by UUID,
  reviewed_at TIMESTAMP WITH TIME ZONE,
  rejection_reason VARCHAR(280),
  loan_id UUID,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_loan_applications_applicant_user_id ON loan_applications(applicant_user_id);
CREATE INDEX IF NOT EXISTS idx_loan_applications_status ON loan_applications(status);

CREATE TABLE IF NOT EXISTS loans (
  id UUID PRIMARY KEY,
  application_id UUID NOT NULL,
  borrower_user_id UUID NOT NULL,
  purpose VARCHAR(60) NOT NULL,
  principal NUMERIC(19, 2) NOT NULL,
  annual_interest_rate NUMERIC(6, 3) NOT NULL,
  term_months INTEGER NOT NULL,
  currency VARCHAR(3) NOT NULL,
  linked_account_id VARCHAR(64) NOT NULL,
  status VARCHAR(20) NOT NULL,
  autopay_enabled BOOLEAN NOT NULL DEFAULT TRUE,
  disbursed_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_loans_borrower_user_id ON loans(borrower_user_id);

CREATE TABLE IF NOT EXISTS loan_installments (
  id UUID PRIMARY KEY,
  loan_id UUID NOT NULL,
  installment_number INTEGER NOT NULL,
  due_date TIMESTAMP WITH TIME ZONE NOT NULL,
  principal_amount NUMERIC(19, 2) NOT NULL,
  interest_amount NUMERIC(19, 2) NOT NULL,
  total_amount NUMERIC(19, 2) NOT NULL,
  remaining_balance_after NUMERIC(19, 2) NOT NULL,
  status VARCHAR(20) NOT NULL,
  paid_at TIMESTAMP WITH TIME ZONE,
  failed_attempts INTEGER NOT NULL DEFAULT 0,
  next_retry_at TIMESTAMP WITH TIME ZONE,
  reminder_sent_at TIMESTAMP WITH TIME ZONE
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_loan_installments_loan_number
  ON loan_installments(loan_id, installment_number);
CREATE INDEX IF NOT EXISTS idx_loan_installments_loan_id ON loan_installments(loan_id);
-- Drives the collection runner's poll (status + due_date/next_retry_at) and the reminder poll
-- (status + reminder_sent_at + due_date). Not a partial/functional index on purpose: those use
-- Postgres-only syntax (WHERE / lower(...)) that H2's test profile rejects outright - see
-- transfer-service's PR history for the migration that shipped broken because of this.
CREATE INDEX IF NOT EXISTS idx_loan_installments_status_due_date ON loan_installments(status, due_date);
