CREATE TABLE purchase_returns (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    purchase_id UUID NOT NULL,
    return_number VARCHAR(40) NOT NULL,
    return_date DATE NOT NULL,
    reason VARCHAR(255),
    notes VARCHAR(255),
    total_amount NUMERIC(19, 2) NOT NULL,
    CONSTRAINT fk_purchase_returns_business
        FOREIGN KEY (business_id) REFERENCES businesses (id),
    CONSTRAINT fk_purchase_returns_purchase
        FOREIGN KEY (purchase_id) REFERENCES purchases (id),
    CONSTRAINT uk_purchase_returns_business_number UNIQUE (business_id, return_number)
);

CREATE INDEX idx_purchase_returns_business_id ON purchase_returns (business_id);
CREATE INDEX idx_purchase_returns_purchase_id ON purchase_returns (purchase_id);
CREATE INDEX idx_purchase_returns_return_date ON purchase_returns (return_date DESC);

CREATE TABLE purchase_return_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    purchase_return_id UUID NOT NULL,
    purchase_item_id UUID NOT NULL,
    product_id UUID NOT NULL,
    quantity NUMERIC(19, 3) NOT NULL,
    unit_cost NUMERIC(19, 2) NOT NULL,
    line_total NUMERIC(19, 2) NOT NULL,
    CONSTRAINT fk_purchase_return_items_return
        FOREIGN KEY (purchase_return_id) REFERENCES purchase_returns (id),
    CONSTRAINT fk_purchase_return_items_purchase_item
        FOREIGN KEY (purchase_item_id) REFERENCES purchase_items (id),
    CONSTRAINT fk_purchase_return_items_product
        FOREIGN KEY (product_id) REFERENCES products (id)
);

CREATE INDEX idx_purchase_return_items_return_id ON purchase_return_items (purchase_return_id);
CREATE INDEX idx_purchase_return_items_purchase_item_id ON purchase_return_items (purchase_item_id);
CREATE INDEX idx_purchase_return_items_product_id ON purchase_return_items (product_id);
