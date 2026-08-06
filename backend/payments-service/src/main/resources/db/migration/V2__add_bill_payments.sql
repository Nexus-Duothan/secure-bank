CREATE TABLE IF NOT EXISTS bill_payments (
  id UUID PRIMARY KEY,
  payer_user_id UUID NOT NULL,
  from_account_id VARCHAR(64) NOT NULL,
  biller_category VARCHAR(50) NOT NULL,
  biller_name VARCHAR(150) NOT NULL,
  reference_number VARCHAR(100) NOT NULL,
  amount NUMERIC(19, 2) NOT NULL,
  currency VARCHAR(3) NOT NULL DEFAULT 'LKR',
  status VARCHAR(20) NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  completed_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_bill_payments_payer_user_id
  ON bill_payments(payer_user_id);
