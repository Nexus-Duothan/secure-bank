-- Accounts & ledger schema (FR-09, FR-10, FR-30).
--
-- Nothing is seeded here on purpose: a brand new customer must see an empty
-- portfolio until they open or claim an account, and every figure the API
-- returns has to come from one of these tables.

CREATE TABLE IF NOT EXISTS accounts (
    id                  VARCHAR(60)   PRIMARY KEY,
    -- NULL means the account was issued by the bank but no customer has claimed
    -- it yet; only an unclaimed account can be linked by a customer.
    user_id             UUID,
    holder_name         VARCHAR(120),
    holder_national_id  VARCHAR(40),
    holder_address_line VARCHAR(180),
    holder_city         VARCHAR(80),
    nickname            VARCHAR(120)  NOT NULL,
    account_type        VARCHAR(20)   NOT NULL,
    product_code        VARCHAR(40),
    product_name        VARCHAR(120),
    account_number      VARCHAR(30)   NOT NULL,
    balance             NUMERIC(19, 2) NOT NULL DEFAULT 0,
    currency            VARCHAR(3)    NOT NULL DEFAULT 'LKR',
    ifsc_code           VARCHAR(20)   NOT NULL,
    opened_on           DATE          NOT NULL,
    home_branch         VARCHAR(80)   NOT NULL,
    ownership_label     VARCHAR(40)   NOT NULL,
    status              VARCHAR(40)   NOT NULL,
    frozen              BOOLEAN       NOT NULL DEFAULT FALSE,
    freeze_reason       VARCHAR(180),
    created_at          TIMESTAMP WITH TIME ZONE     NOT NULL,
    updated_at          TIMESTAMP WITH TIME ZONE     NOT NULL,
    CONSTRAINT uk_accounts_account_number UNIQUE (account_number)
);

CREATE INDEX IF NOT EXISTS idx_accounts_user_id ON accounts (user_id);

CREATE TABLE IF NOT EXISTS account_transactions (
    id               VARCHAR(60)    PRIMARY KEY,
    account_id       VARCHAR(60)    NOT NULL,
    merchant         VARCHAR(140)   NOT NULL,
    category         VARCHAR(60)    NOT NULL,
    transaction_type VARCHAR(40)    NOT NULL,
    location         VARCHAR(120),
    amount           NUMERIC(19, 2) NOT NULL,
    currency         VARCHAR(3)     NOT NULL,
    balance_after    NUMERIC(19, 2),
    occurred_at      TIMESTAMP WITH TIME ZONE      NOT NULL,
    journal_id       VARCHAR(40)    NOT NULL,
    flagged          BOOLEAN        NOT NULL DEFAULT FALSE,
    -- Idempotency key supplied by the calling service (transfer, payments, ...)
    -- so a retried call cannot post the same movement twice.
    reference        VARCHAR(80),
    created_at       TIMESTAMP WITH TIME ZONE      NOT NULL,
    CONSTRAINT fk_account_transactions_account FOREIGN KEY (account_id) REFERENCES accounts (id) ON DELETE CASCADE,
    CONSTRAINT uk_account_transactions_reference UNIQUE (account_id, reference)
);

CREATE INDEX IF NOT EXISTS idx_account_transactions_account_time
    ON account_transactions (account_id, occurred_at DESC);

CREATE TABLE IF NOT EXISTS bank_cards (
    id                 VARCHAR(60)  PRIMARY KEY,
    account_id         VARCHAR(60),
    -- NULL means the card has been issued but not claimed by a customer yet.
    user_id            UUID,
    card_type          VARCHAR(10)  NOT NULL,
    product_name       VARCHAR(80)  NOT NULL,
    card_number        VARCHAR(19)  NOT NULL,
    masked_number      VARCHAR(30)  NOT NULL,
    cardholder_name    VARCHAR(120) NOT NULL,
    expiry_date        VARCHAR(5)   NOT NULL,
    holder_national_id VARCHAR(40),
    scheme             VARCHAR(20)  NOT NULL,
    status             VARCHAR(30)  NOT NULL,
    joint_account_card BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at         TIMESTAMP WITH TIME ZONE    NOT NULL,
    updated_at         TIMESTAMP WITH TIME ZONE    NOT NULL,
    CONSTRAINT uk_bank_cards_card_number UNIQUE (card_number),
    CONSTRAINT fk_bank_cards_account FOREIGN KEY (account_id) REFERENCES accounts (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_bank_cards_account_id ON bank_cards (account_id);
