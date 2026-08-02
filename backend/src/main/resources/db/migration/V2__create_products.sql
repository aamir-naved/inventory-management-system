CREATE TABLE products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    name VARCHAR(150) NOT NULL,
    sku VARCHAR(60),
    category VARCHAR(100),
    unit VARCHAR(30) NOT NULL,
    cost_price NUMERIC(19, 2) NOT NULL,
    selling_price NUMERIC(19, 2) NOT NULL,
    opening_stock NUMERIC(19, 3) NOT NULL,
    current_stock NUMERIC(19, 3) NOT NULL,
    archived BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_products_business
        FOREIGN KEY (business_id) REFERENCES businesses (id),
    CONSTRAINT uk_products_business_sku UNIQUE (business_id, sku)
);

CREATE INDEX idx_products_business_id ON products (business_id);
CREATE INDEX idx_products_business_name ON products (business_id, name);
CREATE INDEX idx_products_business_archived ON products (business_id, archived);
