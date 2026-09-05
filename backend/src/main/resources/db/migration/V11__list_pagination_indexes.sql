CREATE INDEX idx_products_business_archived_name ON products (business_id, archived, name);
CREATE INDEX idx_customers_business_archived_name ON customers (business_id, archived, name);
CREATE INDEX idx_suppliers_business_archived_name ON suppliers (business_id, archived, name);
CREATE INDEX idx_inventory_movements_business_product_created
    ON inventory_movements (business_id, product_id, created_at DESC);
CREATE INDEX idx_sales_business_sale_date ON sales (business_id, sale_date DESC);
CREATE INDEX idx_purchases_business_purchase_date ON purchases (business_id, purchase_date DESC);
