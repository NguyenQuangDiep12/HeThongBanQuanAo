ALTER TABLE order_items
  ADD COLUMN product_name_snapshot VARCHAR(180) NULL;

ALTER TABLE order_items
  ADD COLUMN size_snapshot VARCHAR(40) NULL;

ALTER TABLE order_items
  ADD COLUMN color_snapshot VARCHAR(60) NULL;

UPDATE order_items
SET product_name_snapshot = (
  SELECT p.name
  FROM product_variants pv
  JOIN products p ON p.id = pv.product_id
  WHERE pv.id = order_items.variant_id
);

UPDATE order_items
SET size_snapshot = (
  SELECT pv.size
  FROM product_variants pv
  WHERE pv.id = order_items.variant_id
);

UPDATE order_items
SET color_snapshot = (
  SELECT pv.color
  FROM product_variants pv
  WHERE pv.id = order_items.variant_id
);
