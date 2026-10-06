CREATE TABLE users (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  full_name VARCHAR(120) NOT NULL,
  email VARCHAR(190) NOT NULL UNIQUE,
  password_hash VARCHAR(100) NOT NULL,
  role VARCHAR(20) NOT NULL,
  phone VARCHAR(30),
  created_at TIMESTAMP(6) NOT NULL
);

CREATE TABLE products (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(180) NOT NULL,
  description TEXT,
  price DECIMAL(12,2) NOT NULL,
  image_url VARCHAR(1000),
  status VARCHAR(20) NOT NULL,
  created_at TIMESTAMP(6) NOT NULL,
  CONSTRAINT chk_products_price CHECK (price > 0)
);

CREATE TABLE product_variants (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  product_id BIGINT NOT NULL,
  size VARCHAR(40) NOT NULL,
  color VARCHAR(60) NOT NULL,
  stock_quantity INT NOT NULL,
  price DECIMAL(12,2) NOT NULL,
  CONSTRAINT uq_variant_product_size_color UNIQUE (product_id, size, color),
  CONSTRAINT chk_variant_stock CHECK (stock_quantity >= 0),
  CONSTRAINT chk_variant_price CHECK (price > 0),
  CONSTRAINT fk_variant_product FOREIGN KEY (product_id) REFERENCES products(id)
);

CREATE TABLE carts (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL UNIQUE,
  created_at TIMESTAMP(6) NOT NULL,
  updated_at TIMESTAMP(6) NOT NULL,
  CONSTRAINT fk_cart_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE cart_items (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  cart_id BIGINT NOT NULL,
  variant_id BIGINT NOT NULL,
  quantity INT NOT NULL,
  CONSTRAINT uq_cart_variant UNIQUE (cart_id, variant_id),
  CONSTRAINT chk_cart_quantity CHECK (quantity > 0),
  CONSTRAINT fk_cart_item_cart FOREIGN KEY (cart_id) REFERENCES carts(id) ON DELETE CASCADE,
  CONSTRAINT fk_cart_item_variant FOREIGN KEY (variant_id) REFERENCES product_variants(id)
);

CREATE TABLE orders (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  shipping_address VARCHAR(500) NOT NULL,
  phone VARCHAR(30) NOT NULL,
  total_amount DECIMAL(14,2) NOT NULL,
  status VARCHAR(20) NOT NULL,
  created_at TIMESTAMP(6) NOT NULL,
  CONSTRAINT chk_order_total CHECK (total_amount > 0),
  CONSTRAINT fk_order_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE order_items (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  order_id BIGINT NOT NULL,
  variant_id BIGINT NOT NULL,
  quantity INT NOT NULL,
  unit_price DECIMAL(12,2) NOT NULL,
  sub_total DECIMAL(14,2) NOT NULL,
  CONSTRAINT chk_order_item_quantity CHECK (quantity > 0),
  CONSTRAINT fk_order_item_order FOREIGN KEY (order_id) REFERENCES orders(id),
  CONSTRAINT fk_order_item_variant FOREIGN KEY (variant_id) REFERENCES product_variants(id)
);

CREATE TABLE payments (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  order_id BIGINT NOT NULL UNIQUE,
  payment_method VARCHAR(20) NOT NULL,
  payment_status VARCHAR(20) NOT NULL,
  amount DECIMAL(14,2) NOT NULL,
  paid_at TIMESTAMP(6),
  CONSTRAINT fk_payment_order FOREIGN KEY (order_id) REFERENCES orders(id)
);

CREATE TABLE notifications (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  order_id BIGINT NOT NULL,
  title VARCHAR(180) NOT NULL,
  message VARCHAR(500) NOT NULL,
  type VARCHAR(30) NOT NULL,
  is_read BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP(6) NOT NULL,
  CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_notification_order FOREIGN KEY (order_id) REFERENCES orders(id)
);

CREATE INDEX idx_orders_user_created ON orders(user_id, created_at);
CREATE INDEX idx_notifications_user_created ON notifications(user_id, created_at);
