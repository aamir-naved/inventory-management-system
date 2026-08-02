ALTER TABLE sales
    ADD COLUMN amount_paid NUMERIC(19, 2) NOT NULL DEFAULT 0;

ALTER TABLE purchases
    ADD COLUMN amount_paid NUMERIC(19, 2) NOT NULL DEFAULT 0;

CREATE TABLE payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    party_type VARCHAR(30) NOT NULL,
    party_id UUID NOT NULL,
    document_type VARCHAR(30) NOT NULL,
    document_id UUID NOT NULL,
    payment_date DATE NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    notes VARCHAR(255),
    CONSTRAINT fk_payments_business
        FOREIGN KEY (business_id) REFERENCES businesses (id),
    CONSTRAINT chk_payments_party_type
        CHECK (party_type IN ('CUSTOMER', 'SUPPLIER')),
    CONSTRAINT chk_payments_document_type
        CHECK (document_type IN ('SALE', 'PURCHASE')),
    CONSTRAINT chk_payments_amount_positive
        CHECK (amount > 0)
);

CREATE INDEX idx_payments_business_id ON payments (business_id);
CREATE INDEX idx_payments_document ON payments (business_id, document_type, document_id);
CREATE INDEX idx_payments_party ON payments (business_id, party_type, party_id);
CREATE INDEX idx_payments_payment_date ON payments (payment_date DESC);
