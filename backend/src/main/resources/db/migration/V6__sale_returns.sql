CREATE TABLE sale_returns (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    sale_id UUID NOT NULL,
    return_number VARCHAR(40) NOT NULL,
    return_date DATE NOT NULL,
    reason VARCHAR(255),
    notes VARCHAR(255),
    total_amount NUMERIC(19, 2) NOT NULL,
    CONSTRAINT fk_sale_returns_business
        FOREIGN KEY (business_id) REFERENCES businesses (id),
    CONSTRAINT fk_sale_returns_sale
        FOREIGN KEY (sale_id) REFERENCES sales (id),
    CONSTRAINT uk_sale_returns_business_number UNIQUE (business_id, return_number)
);

CREATE INDEX idx_sale_returns_business_id ON sale_returns (business_id);
CREATE INDEX idx_sale_returns_sale_id ON sale_returns (sale_id);
CREATE INDEX idx_sale_returns_return_date ON sale_returns (return_date DESC);

CREATE TABLE sale_return_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    sale_return_id UUID NOT NULL,
    sale_item_id UUID NOT NULL,
    product_id UUID NOT NULL,
    quantity NUMERIC(19, 3) NOT NULL,
    unit_price NUMERIC(19, 2) NOT NULL,
    line_total NUMERIC(19, 2) NOT NULL,
    CONSTRAINT fk_sale_return_items_return
        FOREIGN KEY (sale_return_id) REFERENCES sale_returns (id),
    CONSTRAINT fk_sale_return_items_sale_item
        FOREIGN KEY (sale_item_id) REFERENCES sale_items (id),
    CONSTRAINT fk_sale_return_items_product
        FOREIGN KEY (product_id) REFERENCES products (id)
);

CREATE INDEX idx_sale_return_items_return_id ON sale_return_items (sale_return_id);
CREATE INDEX idx_sale_return_items_sale_item_id ON sale_return_items (sale_item_id);
CREATE INDEX idx_sale_return_items_product_id ON sale_return_items (product_id);
