#!/usr/bin/env bash
set -euo pipefail

NAMESPACE="${NAMESPACE:-securebank}"
DB_NAME="${DB_NAME:-securebank}"
DB_USER="${DB_USER:-securebank}"
POSTGRES_LABEL="${POSTGRES_LABEL:-app=securebank-postgres}"

DEMO_PASSWORD="SecureBank123!"
DEMO_TOTP_SECRET="JBSWY3DPEHPK3PXP"

echo "============================================================"
echo "SecureBank: seed local Kubernetes database"
echo "============================================================"
echo "Namespace: ${NAMESPACE}"
echo "Database:  ${DB_NAME}"

if ! command -v kubectl >/dev/null 2>&1; then
  echo "Error: kubectl is not installed or not in PATH." >&2
  exit 1
fi

echo "Waiting for Postgres pod..."
kubectl -n "${NAMESPACE}" wait --for=condition=Ready pod -l "${POSTGRES_LABEL}" --timeout=180s >/dev/null

POSTGRES_POD="$(
  kubectl -n "${NAMESPACE}" get pod -l "${POSTGRES_LABEL}" \
    -o jsonpath='{.items[0].metadata.name}'
)"

if [[ -z "${POSTGRES_POD}" ]]; then
  echo "Error: could not find Postgres pod with label ${POSTGRES_LABEL} in namespace ${NAMESPACE}." >&2
  exit 1
fi

# Every table below is created by a service's Flyway migrations on start-up. A missing one means
# some service never ran its migrations - almost always because the cluster is still running an
# older image than the source tree. Say so plainly instead of failing on a raw SQL error.
echo "Checking the schema is up to date..."
REQUIRED_TABLES="users user_profiles accounts account_transactions bank_cards transfers payees \
scheduled_transfers merchants vendor_payments bill_payments loans loan_applications \
loan_installments notifications user_totp_secrets password_reset_tokens"

MISSING=""
for table in ${REQUIRED_TABLES}; do
  if ! kubectl -n "${NAMESPACE}" exec -i "${POSTGRES_POD}" -- \
    psql -U "${DB_USER}" -d "${DB_NAME}" -tAc \
    "SELECT to_regclass('public.${table}') IS NOT NULL" 2>/dev/null | grep -q '^t$'; then
    MISSING="${MISSING} ${table}"
  fi
done

if [[ -n "${MISSING}" ]]; then
  echo "" >&2
  echo "Error: the database is missing these tables:${MISSING}" >&2
  echo "" >&2
  echo "The services that own them have not applied their migrations, which means the" >&2
  echo "cluster is running older images than this source tree. Rebuild and redeploy:" >&2
  echo "" >&2
  echo "  ./scripts/deploy-k8s-local.sh" >&2
  echo "" >&2
  exit 1
fi

echo "Seeding via pod: ${POSTGRES_POD}"

kubectl -n "${NAMESPACE}" exec -i "${POSTGRES_POD}" -- \
  psql -U "${DB_USER}" -d "${DB_NAME}" -v ON_ERROR_STOP=1 <<'SQL'
BEGIN;

TRUNCATE TABLE
  notifications,
  loan_installments,
  loans,
  loan_applications,
  bill_payments,
  vendor_payments,
  merchants,
  scheduled_transfers,
  pending_payee_additions,
  payees,
  transfer_daily_usage,
  transfers,
  bank_cards,
  account_transactions,
  accounts,
  user_totp_scratch_codes,
  user_totp_secrets,
  pending_user_changes,
  user_devices,
  user_profiles,
  password_reset_tokens,
  user_sessions,
  kyc_applications,
  users
RESTART IDENTITY CASCADE;

INSERT INTO users (
  id, username, email, password_hash, national_id_or_passport, full_name,
  phone_number, role, status, mfa_enabled, created_at, updated_at
) VALUES
  ('00000000-0000-4000-8000-000000000101', 'kaveesha', 'kaveesha.customer@securebank.local', '$2a$12$6fd59s3iId1CS/VQKfs17O6PgilNTiqmgEHFo5WiSJNcYpJm.0GqO', 'NIC-901234567V', 'Kaveesha Kapitiarachchi', '+94771111001', 'CUSTOMER', 'ACTIVE', true, now() - interval '45 days', now() - interval '1 hour'),
  ('00000000-0000-4000-8000-000000000102', 'nimal', 'nimal.customer@securebank.local', '$2a$12$6fd59s3iId1CS/VQKfs17O6PgilNTiqmgEHFo5WiSJNcYpJm.0GqO', 'NIC-912345678V', 'Nimal Perera', '+94771111002', 'CUSTOMER', 'ACTIVE', true, now() - interval '40 days', now() - interval '2 hours'),
  ('00000000-0000-4000-8000-000000000103', 'pending.customer', 'pending.customer@securebank.local', '$2a$12$6fd59s3iId1CS/VQKfs17O6PgilNTiqmgEHFo5WiSJNcYpJm.0GqO', 'NIC-923456789V', 'Amani Fernando', '+94771111003', 'CUSTOMER', 'UNDER_REVIEW', true, now() - interval '2 days', now() - interval '30 minutes'),
  ('00000000-0000-4000-8000-000000000201', 'officer', 'officer@securebank.local', '$2a$12$6fd59s3iId1CS/VQKfs17O6PgilNTiqmgEHFo5WiSJNcYpJm.0GqO', 'EMP-BO-001', 'Dilshan Jayawardena', '+94772222001', 'BANK_OFFICER', 'ACTIVE', true, now() - interval '90 days', now() - interval '20 minutes'),
  ('00000000-0000-4000-8000-000000000202', 'admin', 'admin@securebank.local', '$2a$12$6fd59s3iId1CS/VQKfs17O6PgilNTiqmgEHFo5WiSJNcYpJm.0GqO', 'EMP-AD-001', 'SecureBank Admin', '+94773333001', 'ADMIN', 'ACTIVE', true, now() - interval '120 days', now() - interval '15 minutes'),
  ('00000000-0000-4000-8000-000000000301', 'merchantdemo', 'merchant@securebank.local', '$2a$12$6fd59s3iId1CS/VQKfs17O6PgilNTiqmgEHFo5WiSJNcYpJm.0GqO', 'BR-445566', 'Colombo Grocers Pvt Ltd', '+94115550100', 'MERCHANT', 'ACTIVE', true, now() - interval '70 days', now() - interval '10 minutes');

INSERT INTO kyc_applications (
  id, user_id, document_type, document_number, document_payload, status,
  rejection_reason, submitted_at, reviewed_at, reviewed_by
) VALUES
  ('10000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000000101', 'NATIONAL_ID', 'NIC-901234567V', '{"front":"seed/kaveesha-nic-front.png","back":"seed/kaveesha-nic-back.png","addressProof":"seed/kaveesha-utility.pdf"}', 'APPROVED', null, now() - interval '44 days', now() - interval '43 days', 'officer'),
  ('10000000-0000-4000-8000-000000000102', '00000000-0000-4000-8000-000000000102', 'NATIONAL_ID', 'NIC-912345678V', '{"front":"seed/nimal-nic-front.png","back":"seed/nimal-nic-back.png","addressProof":"seed/nimal-bank-letter.pdf"}', 'APPROVED', null, now() - interval '38 days', now() - interval '37 days', 'officer'),
  ('10000000-0000-4000-8000-000000000103', '00000000-0000-4000-8000-000000000103', 'PASSPORT', 'P9234567', '{"passport":"seed/amani-passport.png","selfie":"seed/amani-selfie.png"}', 'UNDER_REVIEW', null, now() - interval '1 day', null, null),
  ('10000000-0000-4000-8000-000000000301', '00000000-0000-4000-8000-000000000301', 'NATIONAL_ID', 'BR-445566', '{"businessRegistration":"seed/colombo-grocers-br.pdf","directorNic":"seed/director-nic.png"}', 'APPROVED', null, now() - interval '69 days', now() - interval '68 days', 'admin');

INSERT INTO user_sessions (
  id, user_id, session_token_hash, refresh_token, ip_address, user_agent,
  device_info, created_at, last_active_at, expires_at, revoked
) VALUES
  ('11000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000000101', 'seed-session-hash-kaveesha', 'seed-refresh-token-kaveesha', '127.0.0.1', 'Seed Browser', 'Chrome on Windows', now() - interval '2 hours', now() - interval '10 minutes', now() + interval '7 days', false),
  ('11000000-0000-4000-8000-000000000201', '00000000-0000-4000-8000-000000000201', 'seed-session-hash-officer', 'seed-refresh-token-officer', '127.0.0.1', 'Seed Browser', 'Chrome on Windows', now() - interval '3 hours', now() - interval '15 minutes', now() + interval '7 days', false),
  ('11000000-0000-4000-8000-000000000301', '00000000-0000-4000-8000-000000000301', 'seed-session-hash-merchant', 'seed-refresh-token-merchant', '127.0.0.1', 'Seed Browser', 'Chrome on Windows', now() - interval '6 hours', now() - interval '1 hour', now() - interval '10 minutes', true);

INSERT INTO password_reset_tokens (
  id, user_id, token, expiry_date, used, created_at
) VALUES
  ('12000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000000101', 'seed-used-reset-token-kaveesha', now() - interval '1 day', true, now() - interval '2 days'),
  ('12000000-0000-4000-8000-000000000102', '00000000-0000-4000-8000-000000000102', 'seed-open-reset-token-nimal', now() + interval '3 hours', false, now() - interval '30 minutes');

INSERT INTO user_profiles (
  id, full_name, email, phone_number, address_line, city, country, language,
  role, status, id_verified, email_notifications, sms_notifications,
  push_notifications, frozen, freeze_reason, frozen_at, created_at, updated_at
) VALUES
  ('00000000-0000-4000-8000-000000000101', 'Kaveesha Kapitiarachchi', 'kaveesha.customer@securebank.local', '+94771111001', '21 Lake Drive', 'Colombo', 'Sri Lanka', 'English', 'CUSTOMER', 'ACTIVE', true, true, true, true, false, null, null, now() - interval '45 days', now() - interval '1 hour'),
  ('00000000-0000-4000-8000-000000000102', 'Nimal Perera', 'nimal.customer@securebank.local', '+94771111002', '18 Temple Road', 'Kandy', 'Sri Lanka', 'Sinhala', 'CUSTOMER', 'ACTIVE', true, true, true, false, false, null, null, now() - interval '40 days', now() - interval '2 hours'),
  ('00000000-0000-4000-8000-000000000103', 'Amani Fernando', 'pending.customer@securebank.local', '+94771111003', '6 Palm Grove', 'Galle', 'Sri Lanka', 'English', 'CUSTOMER', 'PENDING_REVIEW', false, true, true, true, false, null, null, now() - interval '2 days', now() - interval '30 minutes'),
  ('00000000-0000-4000-8000-000000000201', 'Dilshan Jayawardena', 'officer@securebank.local', '+94772222001', 'SecureBank Operations Centre', 'Colombo', 'Sri Lanka', 'English', 'BANK_OFFICER', 'ACTIVE', true, true, false, true, false, null, null, now() - interval '90 days', now() - interval '20 minutes'),
  ('00000000-0000-4000-8000-000000000202', 'SecureBank Admin', 'admin@securebank.local', '+94773333001', 'SecureBank Head Office', 'Colombo', 'Sri Lanka', 'English', 'ADMIN', 'ACTIVE', true, true, false, true, false, null, null, now() - interval '120 days', now() - interval '15 minutes'),
  ('00000000-0000-4000-8000-000000000301', 'Colombo Grocers Pvt Ltd', 'merchant@securebank.local', '+94115550100', '48 Market Street', 'Colombo', 'Sri Lanka', 'English', 'MERCHANT', 'ACTIVE', true, true, true, true, false, null, null, now() - interval '70 days', now() - interval '10 minutes');

INSERT INTO user_devices (
  id, user_profile_id, device_name, device_type, browser, location, trusted,
  last_verified_at, created_at, revoked_at
) VALUES
  ('13000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000000101', 'Kaveesha Laptop', 'Desktop', 'Chrome', 'Colombo, LK', true, now() - interval '1 day', now() - interval '30 days', null),
  ('13000000-0000-4000-8000-000000000102', '00000000-0000-4000-8000-000000000101', 'Kaveesha iPhone', 'Mobile', 'Safari', 'Colombo, LK', false, now() - interval '4 days', now() - interval '12 days', null),
  ('13000000-0000-4000-8000-000000000201', '00000000-0000-4000-8000-000000000102', 'Nimal Android', 'Mobile', 'Chrome', 'Kandy, LK', true, now() - interval '2 days', now() - interval '20 days', null),
  ('13000000-0000-4000-8000-000000000301', '00000000-0000-4000-8000-000000000301', 'Merchant POS Tablet', 'Tablet', 'Chrome', 'Colombo, LK', true, now() - interval '6 hours', now() - interval '45 days', null);

INSERT INTO pending_user_changes (
  id, user_profile_id, type, payload_json, expires_at, confirmed,
  failed_attempts, created_at, confirmed_at
) VALUES
  ('14000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000000101', 'UPDATE_NOTIFICATION_PREFERENCES', '{"emailNotifications":true,"smsNotifications":false,"pushNotifications":true}', now() + interval '20 minutes', false, 0, now() - interval '5 minutes', null),
  ('14000000-0000-4000-8000-000000000103', '00000000-0000-4000-8000-000000000103', 'UPDATE_PROFILE', '{"city":"Matara","language":"Sinhala"}', now() - interval '2 hours', false, 2, now() - interval '1 day', null);

INSERT INTO user_totp_secrets (
  user_id, secret_key, enabled, created_at, updated_at
) VALUES
  ('00000000-0000-4000-8000-000000000101', 'JBSWY3DPEHPK3PXP', true, now() - interval '44 days', now() - interval '44 days'),
  ('00000000-0000-4000-8000-000000000102', 'JBSWY3DPEHPK3PXP', true, now() - interval '39 days', now() - interval '39 days'),
  ('00000000-0000-4000-8000-000000000103', 'JBSWY3DPEHPK3PXP', true, now() - interval '1 day', now() - interval '1 day'),
  ('00000000-0000-4000-8000-000000000201', 'JBSWY3DPEHPK3PXP', true, now() - interval '89 days', now() - interval '89 days'),
  ('00000000-0000-4000-8000-000000000202', 'JBSWY3DPEHPK3PXP', true, now() - interval '119 days', now() - interval '119 days'),
  ('00000000-0000-4000-8000-000000000301', 'JBSWY3DPEHPK3PXP', true, now() - interval '69 days', now() - interval '69 days');

INSERT INTO user_totp_scratch_codes (user_id, scratch_code) VALUES
  ('00000000-0000-4000-8000-000000000101', 'CUST1A01'),
  ('00000000-0000-4000-8000-000000000101', 'CUST1A02'),
  ('00000000-0000-4000-8000-000000000102', 'CUST2A01'),
  ('00000000-0000-4000-8000-000000000102', 'CUST2A02'),
  ('00000000-0000-4000-8000-000000000201', 'OFFICR01'),
  ('00000000-0000-4000-8000-000000000202', 'ADMIN001'),
  ('00000000-0000-4000-8000-000000000301', 'MERCH001');

INSERT INTO accounts (
  id, user_id, holder_name, holder_national_id, holder_address_line,
  holder_city, nickname, account_type, product_code, product_name,
  account_number, balance, currency, ifsc_code, opened_on, home_branch,
  ownership_label, status, frozen, freeze_reason, created_at, updated_at
) VALUES
  ('acc-c1-sav-001', '00000000-0000-4000-8000-000000000101', 'Kaveesha Kapitiarachchi', 'NIC-901234567V', '21 Lake Drive', 'Colombo', 'Everyday Savings', 'SAVINGS', 'SAV-EVERYDAY', 'Everyday Savings', '880000010001', 185500.75, 'LKR', 'SCBLKLX', current_date - 44, 'Colombo Fort', 'Individual', 'Active - Verified', false, null, now() - interval '44 days', now() - interval '30 minutes'),
  ('acc-c1-cur-001', '00000000-0000-4000-8000-000000000101', 'Kaveesha Kapitiarachchi', 'NIC-901234567V', '21 Lake Drive', 'Colombo', 'Salary Current', 'CURRENT', 'CUR-EVERYDAY', 'Everyday Current', '880000010002', 54500.00, 'LKR', 'SCBLKLX', current_date - 28, 'Colombo Fort', 'Individual', 'Active - Verified', false, null, now() - interval '28 days', now() - interval '2 hours'),
  ('acc-c2-sav-001', '00000000-0000-4000-8000-000000000102', 'Nimal Perera', 'NIC-912345678V', '18 Temple Road', 'Kandy', 'Home Savings', 'SAVINGS', 'SAV-SUPER', 'Super Saver', '880000020001', 320750.50, 'LKR', 'SCBLKLX', current_date - 37, 'Kandy City', 'Individual', 'Active - Verified', false, null, now() - interval '37 days', now() - interval '45 minutes'),
  ('acc-c2-cur-001', '00000000-0000-4000-8000-000000000102', 'Nimal Perera', 'NIC-912345678V', '18 Temple Road', 'Kandy', 'Bill Payments', 'CURRENT', 'CUR-EVERYDAY', 'Everyday Current', '880000020002', 89420.00, 'LKR', 'SCBLKLX', current_date - 24, 'Kandy City', 'Individual', 'Active - Verified', false, null, now() - interval '24 days', now() - interval '3 hours'),
  ('acc-merchant-settle-001', '00000000-0000-4000-8000-000000000301', 'Colombo Grocers Pvt Ltd', 'BR-445566', '48 Market Street', 'Colombo', 'Merchant Settlement', 'CURRENT', 'CUR-BUSINESS', 'Business Current', '880000030001', 475250.00, 'LKR', 'SCBLKLX', current_date - 65, 'Colombo Fort', 'Business', 'Active - Verified', false, null, now() - interval '65 days', now() - interval '1 hour'),
  ('acc-unclaimed-sav-001', null, 'Seed Unclaimed Customer', 'NIC-955555555V', 'Unclaimed Address', 'Colombo', 'Unclaimed Savings', 'SAVINGS', 'SAV-EVERYDAY', 'Everyday Savings', '880000090001', 25000.00, 'LKR', 'SCBLKLX', current_date - 10, 'Colombo Fort', 'Individual', 'Issued - Unclaimed', false, null, now() - interval '10 days', now() - interval '10 days');

INSERT INTO account_transactions (
  id, account_id, merchant, category, transaction_type, location, amount,
  currency, balance_after, occurred_at, journal_id, flagged, reference, created_at
) VALUES
  ('txn-c1-sav-001', 'acc-c1-sav-001', 'Opening Deposit', 'Deposit', 'CREDIT', 'Colombo Fort', 150000.00, 'LKR', 150000.00, now() - interval '12 days', 'J-SEED101', false, 'seed-c1-sav-open', now() - interval '12 days'),
  ('txn-c1-sav-002', 'acc-c1-sav-001', 'Acme Payroll', 'Salary', 'CREDIT', 'Colombo', 42500.00, 'LKR', 192500.00, now() - interval '5 days', 'J-SEED102', false, 'seed-c1-salary', now() - interval '5 days'),
  ('txn-c1-sav-003', 'acc-c1-sav-001', 'Colombo Grocers', 'Groceries', 'DEBIT', 'Colombo', -3250.25, 'LKR', 189249.75, now() - interval '3 days', 'J-SEED103', false, 'seed-c1-groceries', now() - interval '3 days'),
  ('txn-c1-sav-004', 'acc-c1-sav-001', 'Transfer to Nimal', 'Transfer', 'DEBIT', 'Online', -5500.00, 'LKR', 183749.75, now() - interval '1 day', 'J-SEED104', false, 'transfer-seed-c1-to-c2', now() - interval '1 day'),
  ('txn-c1-sav-005', 'acc-c1-sav-001', 'Interest Credit', 'Interest', 'CREDIT', 'System', 1751.00, 'LKR', 185500.75, now() - interval '6 hours', 'J-SEED105', false, 'seed-c1-interest', now() - interval '6 hours'),
  ('txn-c1-cur-001', 'acc-c1-cur-001', 'Salary Sweep', 'Deposit', 'CREDIT', 'Online', 60000.00, 'LKR', 60000.00, now() - interval '8 days', 'J-SEED111', false, 'seed-c1-cur-open', now() - interval '8 days'),
  ('txn-c1-cur-002', 'acc-c1-cur-001', 'Dialog Bill Payment', 'Utilities', 'DEBIT', 'Online', -2500.00, 'LKR', 57500.00, now() - interval '4 days', 'J-SEED112', false, 'payment-seed-dialog', now() - interval '4 days'),
  ('txn-c1-cur-003', 'acc-c1-cur-001', 'ATM Withdrawal', 'Cash', 'DEBIT', 'Colombo 03', -3000.00, 'LKR', 54500.00, now() - interval '2 days', 'J-SEED113', false, 'seed-c1-atm', now() - interval '2 days'),
  ('txn-c2-sav-001', 'acc-c2-sav-001', 'Opening Deposit', 'Deposit', 'CREDIT', 'Kandy City', 250000.00, 'LKR', 250000.00, now() - interval '14 days', 'J-SEED201', false, 'seed-c2-sav-open', now() - interval '14 days'),
  ('txn-c2-sav-002', 'acc-c2-sav-001', 'Consulting Income', 'Income', 'CREDIT', 'Online', 78500.50, 'LKR', 328500.50, now() - interval '7 days', 'J-SEED202', false, 'seed-c2-income', now() - interval '7 days'),
  ('txn-c2-sav-003', 'acc-c2-sav-001', 'Card Purchase', 'Shopping', 'DEBIT', 'Kandy', -7750.00, 'LKR', 320750.50, now() - interval '2 days', 'J-SEED203', false, 'seed-c2-shopping', now() - interval '2 days'),
  ('txn-c2-cur-001', 'acc-c2-cur-001', 'Account Funding', 'Deposit', 'CREDIT', 'Kandy City', 100000.00, 'LKR', 100000.00, now() - interval '10 days', 'J-SEED211', false, 'seed-c2-cur-open', now() - interval '10 days'),
  ('txn-c2-cur-002', 'acc-c2-cur-001', 'Loan Installment', 'Loan', 'DEBIT', 'System', -10580.00, 'LKR', 89420.00, now() - interval '3 days', 'J-SEED212', false, 'loan-seed-c2-installment', now() - interval '3 days'),
  ('txn-merchant-001', 'acc-merchant-settle-001', 'QR Payment Settlement', 'Merchant Settlement', 'CREDIT', 'Colombo', 15250.00, 'LKR', 475250.00, now() - interval '1 day', 'J-SEED301', false, 'merchant-settlement-001', now() - interval '1 day');

INSERT INTO bank_cards (
  id, account_id, user_id, card_type, product_name, card_number, masked_number,
  cardholder_name, expiry_date, holder_national_id, scheme, status,
  joint_account_card, created_at, updated_at
) VALUES
  ('card-c1-debit-sav', 'acc-c1-sav-001', '00000000-0000-4000-8000-000000000101', 'DEBIT', 'Everyday Savings Debit', '4910120000010001', '4910 12** **** 0001', 'KAVEESHA KAPITIARACHCHI', '12/30', 'NIC-901234567V', 'VISA', 'Active', false, now() - interval '44 days', now() - interval '1 day'),
  ('card-c1-debit-cur', 'acc-c1-cur-001', '00000000-0000-4000-8000-000000000101', 'DEBIT', 'Everyday Current Debit', '4910120000010002', '4910 12** **** 0002', 'KAVEESHA KAPITIARACHCHI', '11/30', 'NIC-901234567V', 'VISA', 'Active', false, now() - interval '28 days', now() - interval '2 days'),
  ('card-c2-debit-sav', 'acc-c2-sav-001', '00000000-0000-4000-8000-000000000102', 'DEBIT', 'Super Saver Debit', '4910120000020001', '4910 12** **** 0001', 'NIMAL PERERA', '10/30', 'NIC-912345678V', 'MASTERCARD', 'Active', false, now() - interval '37 days', now() - interval '1 day'),
  ('card-c2-credit-unlinked', null, null, 'CREDIT', 'SecureBank Platinum Credit', '5522330000090009', '5522 33** **** 0009', 'NIMAL PERERA', '09/30', 'NIC-912345678V', 'MASTERCARD', 'Issued - Ready to Link', false, now() - interval '4 days', now() - interval '4 days');

INSERT INTO transfers (
  id, initiated_by_user_id, from_account_id, to_account, amount, fee, currency,
  note, status, failure_reason, idempotency_key, created_at, confirmed_at
) VALUES
  ('20000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000000101', 'acc-c1-sav-001', '880000020001', 5500.00, 25.00, 'LKR', 'Dinner split', 'COMPLETED', null, 'seed-transfer-c1-to-c2', now() - interval '1 day', now() - interval '1 day' + interval '2 minutes'),
  ('20000000-0000-4000-8000-000000000102', '00000000-0000-4000-8000-000000000102', 'acc-c2-cur-001', '880000010001', 12500.00, 25.00, 'LKR', 'Rent contribution', 'PENDING_CONFIRMATION', null, 'seed-transfer-pending-c2', now() - interval '45 minutes', null),
  ('20000000-0000-4000-8000-000000000103', '00000000-0000-4000-8000-000000000101', 'acc-c1-cur-001', 'EXT-778899', 75000.00, 50.00, 'LKR', 'Large transfer review demo', 'REJECTED', 'Rejected by fraud/risk policy', 'seed-transfer-rejected', now() - interval '4 days', null),
  ('20000000-0000-4000-8000-000000000104', '00000000-0000-4000-8000-000000000102', 'acc-c2-sav-001', '880000010002', 999999.00, 50.00, 'LKR', 'Insufficient funds demo', 'FAILED', 'Insufficient available balance', 'seed-transfer-failed', now() - interval '6 days', null);

INSERT INTO transfer_daily_usage (account_id, usage_date, total_amount) VALUES
  ('acc-c1-sav-001', current_date, 5500.00),
  ('acc-c2-cur-001', current_date, 12500.00),
  ('acc-c1-cur-001', current_date - 4, 75000.00);

INSERT INTO payees (
  id, owner_user_id, nickname, account_reference, cooling_off_until, created_at
) VALUES
  ('21000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000000101', 'Nimal Savings', '880000020001', now() - interval '10 days', now() - interval '15 days'),
  ('21000000-0000-4000-8000-000000000102', '00000000-0000-4000-8000-000000000101', 'Dialog Mobile Bill', 'DIALOG-0711111001', now() + interval '18 hours', now() - interval '6 hours'),
  ('21000000-0000-4000-8000-000000000201', '00000000-0000-4000-8000-000000000102', 'Kaveesha Savings', '880000010001', now() - interval '3 days', now() - interval '9 days');

INSERT INTO pending_payee_additions (
  id, owner_user_id, nickname, account_reference, expires_at, confirmed,
  failed_attempts, created_at, confirmed_at
) VALUES
  ('22000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000000101', 'Water Board', 'WATER-ACC-7788', now() + interval '12 minutes', false, 0, now() - interval '3 minutes', null),
  ('22000000-0000-4000-8000-000000000102', '00000000-0000-4000-8000-000000000102', 'Expired Insurance Payee', 'INS-ACC-9000', now() - interval '1 hour', false, 1, now() - interval '2 hours', null);

INSERT INTO scheduled_transfers (
  id, owner_user_id, from_account_id, to_account, amount, note, frequency,
  next_run_at, end_date, status, last_executed_at, last_execution_status, created_at
) VALUES
  ('23000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000000101', 'acc-c1-cur-001', '880000020001', 10000.00, 'Monthly family support', 'MONTHLY', now() + interval '4 days', now() + interval '10 months', 'ACTIVE', now() - interval '26 days', 'COMPLETED', now() - interval '2 months'),
  ('23000000-0000-4000-8000-000000000102', '00000000-0000-4000-8000-000000000102', 'acc-c2-sav-001', '880000010002', 2500.00, 'Weekly savings transfer', 'WEEKLY', now() + interval '2 days', null, 'PAUSED', now() - interval '9 days', 'PAUSED_BY_CUSTOMER', now() - interval '1 month'),
  ('23000000-0000-4000-8000-000000000103', '00000000-0000-4000-8000-000000000101', 'acc-c1-sav-001', '880000020002', 1500.00, 'One-time charity payment', 'ONE_TIME', now() - interval '1 day', null, 'FAILED', null, 'Insufficient confirmation window', now() - interval '5 days');

INSERT INTO merchants (
  id, merchant_code, business_name, category, settlement_account_id,
  merchant_user_id, active, created_at, updated_at
) VALUES
  ('30000000-0000-4000-8000-000000000301', 'CGROCERS', 'Colombo Grocers Pvt Ltd', 'Groceries', 'acc-merchant-settle-001', '00000000-0000-4000-8000-000000000301', true, now() - interval '68 days', now() - interval '1 hour'),
  ('30000000-0000-4000-8000-000000000302', 'DIALOGUTIL', 'Dialog Mobile Utilities', 'Utilities', 'acc-merchant-settle-001', null, true, now() - interval '180 days', now() - interval '8 days'),
  ('30000000-0000-4000-8000-000000000303', 'TRAVELHUB', 'Travel Hub Lanka', 'Travel', 'acc-merchant-settle-001', null, false, now() - interval '220 days', now() - interval '30 days');

INSERT INTO vendor_payments (
  id, payer_user_id, merchant_id, amount, currency, channel, status, note,
  reference_number, failure_reason, reviewed_by, reviewed_at, created_at, updated_at
) VALUES
  ('31000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000000101', '30000000-0000-4000-8000-000000000301', 3250.25, 'LKR', 'QR', 'COMPLETED', 'Weekly groceries', 'PAY-SEED-1001', null, null, null, now() - interval '3 days', now() - interval '3 days'),
  ('31000000-0000-4000-8000-000000000102', '00000000-0000-4000-8000-000000000101', '30000000-0000-4000-8000-000000000302', 2500.00, 'LKR', 'DIRECT', 'COMPLETED', 'Mobile bill', 'PAY-SEED-1002', null, null, null, now() - interval '4 days', now() - interval '4 days'),
  ('31000000-0000-4000-8000-000000000103', '00000000-0000-4000-8000-000000000102', '30000000-0000-4000-8000-000000000301', 48750.00, 'LKR', 'QR', 'HELD_FOR_REVIEW', 'Bulk stock purchase', 'PAY-SEED-1003', null, '00000000-0000-4000-8000-000000000201', now() - interval '1 hour', now() - interval '2 hours', now() - interval '1 hour'),
  ('31000000-0000-4000-8000-000000000104', '00000000-0000-4000-8000-000000000102', '30000000-0000-4000-8000-000000000303', 18000.00, 'LKR', 'DIRECT', 'FAILED', 'Travel booking attempt', 'PAY-SEED-1004', 'Merchant is inactive', null, null, now() - interval '5 days', now() - interval '5 days');

INSERT INTO bill_payments (
  id, payer_user_id, from_account_id, biller_category, biller_name,
  reference_number, amount, currency, status, created_at, completed_at
) VALUES
  ('32000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000000101', 'acc-c1-cur-001', 'Electricity', 'Ceylon Electricity Board', 'CEB-204883190', 6120.00, 'LKR', 'COMPLETED', now() - interval '6 days', now() - interval '6 days'),
  ('32000000-0000-4000-8000-000000000102', '00000000-0000-4000-8000-000000000102', 'acc-c2-cur-001', 'Mobile', 'Dialog Axiata', 'DIALOG-0775550199', 1250.00, 'LKR', 'COMPLETED', now() - interval '2 days', now() - interval '2 days');

INSERT INTO loan_applications (
  id, applicant_user_id, purpose, amount, term_months, annual_interest_rate,
  linked_account_id, status, reviewed_by, reviewed_at, rejection_reason,
  loan_id, created_at
) VALUES
  ('40000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000000101', 'HOME_RENOVATION', 600000.00, 12, 13.500, 'acc-c1-sav-001', 'DISBURSED', '00000000-0000-4000-8000-000000000201', now() - interval '20 days', null, '41000000-0000-4000-8000-000000000101', now() - interval '22 days'),
  ('40000000-0000-4000-8000-000000000102', '00000000-0000-4000-8000-000000000102', 'EDUCATION', 250000.00, 6, 11.250, 'acc-c2-sav-001', 'APPROVED', '00000000-0000-4000-8000-000000000201', now() - interval '2 days', null, null, now() - interval '4 days'),
  ('40000000-0000-4000-8000-000000000103', '00000000-0000-4000-8000-000000000102', 'TRAVEL', 900000.00, 24, 15.000, 'acc-c2-cur-001', 'REJECTED', '00000000-0000-4000-8000-000000000201', now() - interval '12 days', 'Debt service ratio is above policy threshold', null, now() - interval '13 days'),
  ('40000000-0000-4000-8000-000000000104', '00000000-0000-4000-8000-000000000101', 'VEHICLE', 1200000.00, 36, 14.500, 'acc-c1-cur-001', 'UNDER_REVIEW', null, null, null, null, now() - interval '12 hours');

INSERT INTO loans (
  id, application_id, borrower_user_id, purpose, principal, annual_interest_rate,
  term_months, currency, linked_account_id, status, autopay_enabled, disbursed_at
) VALUES
  ('41000000-0000-4000-8000-000000000101', '40000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000000101', 'HOME_RENOVATION', 600000.00, 13.500, 12, 'LKR', 'acc-c1-sav-001', 'ACTIVE', true, now() - interval '20 days'),
  ('41000000-0000-4000-8000-000000000102', '40000000-0000-4000-8000-000000000102', '00000000-0000-4000-8000-000000000102', 'EDUCATION', 250000.00, 11.250, 6, 'LKR', 'acc-c2-sav-001', 'ACTIVE', false, now() - interval '1 day');

INSERT INTO loan_installments (
  id, loan_id, installment_number, due_date, principal_amount, interest_amount,
  total_amount, remaining_balance_after, status, paid_at, failed_attempts,
  next_retry_at, reminder_sent_at
) VALUES
  ('42000000-0000-4000-8000-000000000101', '41000000-0000-4000-8000-000000000101', 1, now() - interval '5 days', 50000.00, 6750.00, 56750.00, 550000.00, 'PAID', now() - interval '4 days', 0, null, now() - interval '8 days'),
  ('42000000-0000-4000-8000-000000000102', '41000000-0000-4000-8000-000000000101', 2, now() + interval '25 days', 50000.00, 6187.50, 56187.50, 500000.00, 'PENDING', null, 0, null, null),
  ('42000000-0000-4000-8000-000000000103', '41000000-0000-4000-8000-000000000101', 3, now() + interval '55 days', 50000.00, 5625.00, 55625.00, 450000.00, 'PENDING', null, 0, null, null),
  ('42000000-0000-4000-8000-000000000201', '41000000-0000-4000-8000-000000000102', 1, now() - interval '3 days', 41666.67, 2343.75, 44010.42, 208333.33, 'FAILED', null, 1, now() + interval '1 hour', now() - interval '6 days'),
  ('42000000-0000-4000-8000-000000000202', '41000000-0000-4000-8000-000000000102', 2, now() + interval '27 days', 41666.67, 1953.13, 43619.80, 166666.66, 'PENDING', null, 0, null, null);

INSERT INTO notifications (
  id, user_id, type, channel, title, message, "read", created_at, metadata_json
) VALUES
  ('50000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000000101', 'TRANSACTION_ALERT', 'IN_APP', 'Transfer completed', 'Your transfer of LKR 5,500.00 to Nimal Savings was completed.', false, now() - interval '1 day', '{"transferId":"20000000-0000-4000-8000-000000000101"}'),
  ('50000000-0000-4000-8000-000000000102', '00000000-0000-4000-8000-000000000101', 'SECURITY_ALERT', 'EMAIL', 'New trusted device', 'A trusted device was added to your profile.', true, now() - interval '12 days', '{"deviceId":"13000000-0000-4000-8000-000000000101"}'),
  ('50000000-0000-4000-8000-000000000103', '00000000-0000-4000-8000-000000000101', 'ACCOUNT_ALERT', 'SMS', 'Loan installment reminder', 'Your next home renovation loan installment is due soon.', false, now() - interval '1 hour', '{"loanId":"41000000-0000-4000-8000-000000000101"}'),
  ('50000000-0000-4000-8000-000000000201', '00000000-0000-4000-8000-000000000102', 'TRANSACTION_ALERT', 'PUSH', 'Payment held for review', 'Your LKR 48,750.00 merchant payment is being reviewed.', false, now() - interval '2 hours', '{"paymentId":"31000000-0000-4000-8000-000000000103"}'),
  ('50000000-0000-4000-8000-000000000202', '00000000-0000-4000-8000-000000000102', 'SYSTEM_NOTICE', 'IN_APP', 'Loan approved', 'Your education loan application has been approved.', true, now() - interval '2 days', '{"applicationId":"40000000-0000-4000-8000-000000000102"}'),
  ('50000000-0000-4000-8000-000000000301', '00000000-0000-4000-8000-000000000301', 'TRANSACTION_ALERT', 'IN_APP', 'Settlement received', 'Your latest merchant settlement has posted.', false, now() - interval '1 day', '{"accountId":"acc-merchant-settle-001"}'),
  ('50000000-0000-4000-8000-000000000401', '00000000-0000-4000-8000-000000000201', 'SYSTEM_NOTICE', 'IN_APP', 'KYC review pending', 'Amani Fernando is waiting for identity review.', false, now() - interval '30 minutes', '{"kycApplicationId":"10000000-0000-4000-8000-000000000103"}');

COMMIT;

SELECT 'users' AS table_name, count(*) AS seeded_rows FROM users
UNION ALL SELECT 'user_profiles', count(*) FROM user_profiles
UNION ALL SELECT 'accounts', count(*) FROM accounts
UNION ALL SELECT 'account_transactions', count(*) FROM account_transactions
UNION ALL SELECT 'bank_cards', count(*) FROM bank_cards
UNION ALL SELECT 'transfers', count(*) FROM transfers
UNION ALL SELECT 'payees', count(*) FROM payees
UNION ALL SELECT 'scheduled_transfers', count(*) FROM scheduled_transfers
UNION ALL SELECT 'merchants', count(*) FROM merchants
UNION ALL SELECT 'vendor_payments', count(*) FROM vendor_payments
UNION ALL SELECT 'bill_payments', count(*) FROM bill_payments
UNION ALL SELECT 'loan_applications', count(*) FROM loan_applications
UNION ALL SELECT 'loans', count(*) FROM loans
UNION ALL SELECT 'loan_installments', count(*) FROM loan_installments
UNION ALL SELECT 'notifications', count(*) FROM notifications
UNION ALL SELECT 'user_totp_secrets', count(*) FROM user_totp_secrets
ORDER BY table_name;
SQL

echo "============================================================"
echo "Seed complete."
echo
echo "Demo login password for all seeded users: ${DEMO_PASSWORD}"
echo "Demo TOTP setup key for all seeded users: ${DEMO_TOTP_SECRET}"
echo
echo "Seeded users:"
echo "  CUSTOMER:      kaveesha"
echo "  CUSTOMER:      nimal"
echo "  CUSTOMER KYC:  pending.customer"
echo "  BANK_OFFICER:  officer"
echo "  ADMIN:         admin"
echo "  MERCHANT:      merchantdemo"
echo "============================================================"
