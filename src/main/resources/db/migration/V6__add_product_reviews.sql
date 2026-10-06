CREATE TABLE product_reviews (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  rating INT NOT NULL,
  comment VARCHAR(1000),
  created_at TIMESTAMP(6) NOT NULL,
  CONSTRAINT chk_product_review_rating CHECK (rating BETWEEN 1 AND 5),
  CONSTRAINT uq_product_review_user_product UNIQUE (user_id, product_id),
  CONSTRAINT fk_product_review_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_product_review_product FOREIGN KEY (product_id) REFERENCES products(id)
);

CREATE INDEX idx_product_reviews_product_created ON product_reviews(product_id, created_at);
