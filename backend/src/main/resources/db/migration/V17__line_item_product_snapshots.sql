ALTER TABLE sale_items
    ADD COLUMN product_name VARCHAR(150),
    ADD COLUMN unit VARCHAR(30);

UPDATE sale_items
SET product_name = (SELECT p.name FROM products p WHERE p.id = sale_items.product_id),
    unit = (SELECT p.unit FROM products p WHERE p.id = sale_items.product_id);

ALTER TABLE sale_items
    ALTER COLUMN product_name SET NOT NULL,
    ALTER COLUMN unit SET NOT NULL;

ALTER TABLE purchase_items
    ADD COLUMN product_name VARCHAR(150),
    ADD COLUMN unit VARCHAR(30);

UPDATE purchase_items
SET product_name = (SELECT p.name FROM products p WHERE p.id = purchase_items.product_id),
    unit = (SELECT p.unit FROM products p WHERE p.id = purchase_items.product_id);

ALTER TABLE purchase_items
    ALTER COLUMN product_name SET NOT NULL,
    ALTER COLUMN unit SET NOT NULL;

ALTER TABLE sale_return_items
    ADD COLUMN product_name VARCHAR(150),
    ADD COLUMN unit VARCHAR(30);

UPDATE sale_return_items
SET product_name = (SELECT p.name FROM products p WHERE p.id = sale_return_items.product_id),
    unit = (SELECT p.unit FROM products p WHERE p.id = sale_return_items.product_id);

ALTER TABLE sale_return_items
    ALTER COLUMN product_name SET NOT NULL,
    ALTER COLUMN unit SET NOT NULL;

ALTER TABLE purchase_return_items
    ADD COLUMN product_name VARCHAR(150),
    ADD COLUMN unit VARCHAR(30);

UPDATE purchase_return_items
SET product_name = (SELECT p.name FROM products p WHERE p.id = purchase_return_items.product_id),
    unit = (SELECT p.unit FROM products p WHERE p.id = purchase_return_items.product_id);

ALTER TABLE purchase_return_items
    ALTER COLUMN product_name SET NOT NULL,
    ALTER COLUMN unit SET NOT NULL;
