CREATE TABLE IF NOT EXISTS bank_accounts (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  bank_code VARCHAR(20) NOT NULL,
  bank_name VARCHAR(100) NOT NULL,
  account_no VARCHAR(50) NOT NULL,
  account_name VARCHAR(120) NOT NULL,
  qr_template VARCHAR(20),
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP(6) NOT NULL
);

CREATE TABLE IF NOT EXISTS payment_transactions (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  order_id BIGINT NOT NULL,
  order_code VARCHAR(50) NOT NULL,
  product_sample_name VARCHAR(100),
  random_code VARCHAR(20) NOT NULL,
  transfer_memo VARCHAR(100) NOT NULL,
  amount DECIMAL(14,2) NOT NULL,
  bank_account_no VARCHAR(50),
  status VARCHAR(30) NOT NULL,
  univer_row_id VARCHAR(50),
  created_at TIMESTAMP(6) NOT NULL,
  confirmed_at TIMESTAMP(6) NULL,
  CONSTRAINT fk_payment_transaction_order
    FOREIGN KEY (order_id) REFERENCES orders(id),
  INDEX idx_payment_transactions_order (order_id)
);
