-- Payments Service Initial Database Schema (V1)

CREATE TABLE IF NOT EXISTS merchants (
  id UUID PRIMARY KEY,
  merchant_code VARCHAR(30) NOT NULL UNIQUE,
  business_name VARCHAR(150) NOT NULL,
  category VARCHAR(50),
  settlement_account_id VARCHAR(50) NOT NULL,
  merchant_user_id UUID,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS vendor_payments (
  id UUID PRIMARY KEY,
  payer_user_id UUID NOT NULL,
  merchant_id UUID NOT NULL REFERENCES merchants(id),
  amount NUMERIC(19, 4) NOT NULL,
  currency VARCHAR(3) NOT NULL,
  channel VARCHAR(20) NOT NULL,
  status VARCHAR(20) NOT NULL,
  note VARCHAR(255),
  reference_number VARCHAR(40) UNIQUE,
  failure_reason VARCHAR(255),
  reviewed_by UUID,
  reviewed_at TIMESTAMP WITH TIME ZONE,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for fast query performance
CREATE INDEX IF NOT EXISTS idx_merchants_merchant_code ON merchants(merchant_code);
CREATE INDEX IF NOT EXISTS idx_merchants_merchant_user_id ON merchants(merchant_user_id);
CREATE INDEX IF NOT EXISTS idx_vendor_payments_payer_user_id ON vendor_payments(payer_user_id);
CREATE INDEX IF NOT EXISTS idx_vendor_payments_merchant_id ON vendor_payments(merchant_id);
CREATE INDEX IF NOT EXISTS idx_vendor_payments_status ON vendor_payments(status);
CREATE UNIQUE INDEX IF NOT EXISTS idx_vendor_payments_reference_number ON vendor_payments(reference_number);
