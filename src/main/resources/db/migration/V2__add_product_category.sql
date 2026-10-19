ALTER TABLE products
  ADD COLUMN category VARCHAR(80) NOT NULL DEFAULT 'Thời trang' AFTER name;
