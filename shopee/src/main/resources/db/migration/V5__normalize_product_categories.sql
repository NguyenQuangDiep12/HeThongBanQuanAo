CREATE TABLE categories (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(80) NOT NULL,
  description VARCHAR(500),
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP(6) NOT NULL,
  CONSTRAINT uq_categories_name UNIQUE (name)
);

INSERT INTO categories (name, active, created_at)
SELECT DISTINCT TRIM(category), TRUE, CURRENT_TIMESTAMP(6)
FROM products
WHERE category IS NOT NULL
  AND TRIM(category) <> '';

INSERT INTO categories (name, active, created_at)
SELECT 'Thời trang', TRUE, CURRENT_TIMESTAMP(6)
WHERE NOT EXISTS (
  SELECT 1 FROM categories WHERE name = 'Thời trang'
);

ALTER TABLE products
  ADD COLUMN category_id BIGINT NULL AFTER name;

UPDATE products p
JOIN categories c ON c.name = TRIM(p.category)
SET p.category_id = c.id;

UPDATE products p
JOIN categories c ON c.name = 'Thời trang'
SET p.category_id = c.id
WHERE p.category_id IS NULL;

ALTER TABLE products
  MODIFY COLUMN category_id BIGINT NOT NULL;

ALTER TABLE products
  ADD INDEX idx_products_category_id (category_id);

ALTER TABLE products
  ADD CONSTRAINT fk_product_category
    FOREIGN KEY (category_id) REFERENCES categories(id);

ALTER TABLE products
  DROP COLUMN category;
