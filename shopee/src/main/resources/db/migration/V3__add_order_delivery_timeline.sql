ALTER TABLE orders
  ADD COLUMN confirmed_at TIMESTAMP(6) NULL;

ALTER TABLE orders
  ADD COLUMN shipping_at TIMESTAMP(6) NULL;

ALTER TABLE orders
  ADD COLUMN delivered_at TIMESTAMP(6) NULL;

ALTER TABLE orders
  ADD COLUMN customer_confirmed_at TIMESTAMP(6) NULL;

ALTER TABLE orders
  ADD COLUMN cancelled_at TIMESTAMP(6) NULL;
