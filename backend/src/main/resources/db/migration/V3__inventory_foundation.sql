ALTER TABLE products
    ADD COLUMN low_stock_threshold NUMERIC(19, 3) NOT NULL DEFAULT 0;

CREATE TABLE inventory_movements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id UUID NOT NULL,
    product_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    movement_type VARCHAR(50) NOT NULL,
    quantity_change NUMERIC(19, 3) NOT NULL,
    quantity_before NUMERIC(19, 3) NOT NULL,
    quantity_after NUMERIC(19, 3) NOT NULL,
    notes VARCHAR(255),
    CONSTRAINT fk_inventory_movements_business
        FOREIGN KEY (business_id) REFERENCES businesses (id),
    CONSTRAINT fk_inventory_movements_product
        FOREIGN KEY (product_id) REFERENCES products (id)
);

CREATE INDEX idx_inventory_movements_business_id ON inventory_movements (business_id);
CREATE INDEX idx_inventory_movements_product_id ON inventory_movements (product_id);
CREATE INDEX idx_inventory_movements_created_at ON inventory_movements (created_at DESC);
