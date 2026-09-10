CREATE TABLE document_sequences (
    id UUID PRIMARY KEY,
    business_id UUID NOT NULL REFERENCES businesses (id),
    document_type VARCHAR(30) NOT NULL,
    financial_year VARCHAR(9) NOT NULL,
    last_value BIGINT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_document_sequences_business_type_fy
        UNIQUE (business_id, document_type, financial_year)
);

CREATE INDEX idx_document_sequences_business
    ON document_sequences (business_id);
