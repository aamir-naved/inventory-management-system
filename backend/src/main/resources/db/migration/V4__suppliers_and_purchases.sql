CREATE TABLE suppliers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    name VARCHAR(150) NOT NULL,
    contact_person VARCHAR(120),
    mobile_number VARCHAR(20),
    address_line VARCHAR(255),
    archived BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_suppliers_business
        FOREIGN KEY (business_id) REFERENCES businesses (id)
);

CREATE INDEX idx_suppliers_business_id ON suppliers (business_id);
CREATE INDEX idx_suppliers_business_name ON suppliers (business_id, name);

CREATE TABLE purchases (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    purchase_number VARCHAR(40) NOT NULL,
    supplier_id UUID NOT NULL,
    purchase_date DATE NOT NULL,
    payment_status VARCHAR(30) NOT NULL,
    notes VARCHAR(255),
    total_amount NUMERIC(19, 2) NOT NULL,
    cancelled BOOLEAN NOT NULL DEFAULT FALSE,
    cancellation_reason VARCHAR(255),
    CONSTRAINT fk_purchases_business
        FOREIGN KEY (business_id) REFERENCES businesses (id),
    CONSTRAINT fk_purchases_supplier
        FOREIGN KEY (supplier_id) REFERENCES suppliers (id),
    CONSTRAINT uk_purchases_business_number UNIQUE (business_id, purchase_number)
);

CREATE INDEX idx_purchases_business_id ON purchases (business_id);
CREATE INDEX idx_purchases_supplier_id ON purchases (supplier_id);
CREATE INDEX idx_purchases_purchase_date ON purchases (purchase_date DESC);

CREATE TABLE purchase_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    purchase_id UUID NOT NULL,
    product_id UUID NOT NULL,
    quantity NUMERIC(19, 3) NOT NULL,
    unit_cost NUMERIC(19, 2) NOT NULL,
    line_total NUMERIC(19, 2) NOT NULL,
    CONSTRAINT fk_purchase_items_purchase
        FOREIGN KEY (purchase_id) REFERENCES purchases (id),
    CONSTRAINT fk_purchase_items_product
        FOREIGN KEY (product_id) REFERENCES products (id)
);

CREATE INDEX idx_purchase_items_purchase_id ON purchase_items (purchase_id);
CREATE INDEX idx_purchase_items_product_id ON purchase_items (product_id);
