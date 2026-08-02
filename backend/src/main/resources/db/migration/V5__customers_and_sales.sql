CREATE TABLE customers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    name VARCHAR(150) NOT NULL,
    contact_person VARCHAR(120),
    mobile_number VARCHAR(20),
    address_line VARCHAR(255),
    archived BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_customers_business
        FOREIGN KEY (business_id) REFERENCES businesses (id)
);

CREATE INDEX idx_customers_business_id ON customers (business_id);
CREATE INDEX idx_customers_business_name ON customers (business_id, name);

CREATE TABLE sales (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    sale_number VARCHAR(40) NOT NULL,
    customer_id UUID NOT NULL,
    sale_date DATE NOT NULL,
    payment_status VARCHAR(30) NOT NULL,
    notes VARCHAR(255),
    total_amount NUMERIC(19, 2) NOT NULL,
    cancelled BOOLEAN NOT NULL DEFAULT FALSE,
    cancellation_reason VARCHAR(255),
    CONSTRAINT fk_sales_business
        FOREIGN KEY (business_id) REFERENCES businesses (id),
    CONSTRAINT fk_sales_customer
        FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT uk_sales_business_number UNIQUE (business_id, sale_number)
);

CREATE INDEX idx_sales_business_id ON sales (business_id);
CREATE INDEX idx_sales_customer_id ON sales (customer_id);
CREATE INDEX idx_sales_sale_date ON sales (sale_date DESC);

CREATE TABLE sale_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    sale_id UUID NOT NULL,
    product_id UUID NOT NULL,
    quantity NUMERIC(19, 3) NOT NULL,
    unit_price NUMERIC(19, 2) NOT NULL,
    line_total NUMERIC(19, 2) NOT NULL,
    CONSTRAINT fk_sale_items_sale
        FOREIGN KEY (sale_id) REFERENCES sales (id),
    CONSTRAINT fk_sale_items_product
        FOREIGN KEY (product_id) REFERENCES products (id)
);

CREATE INDEX idx_sale_items_sale_id ON sale_items (sale_id);
CREATE INDEX idx_sale_items_product_id ON sale_items (product_id);
