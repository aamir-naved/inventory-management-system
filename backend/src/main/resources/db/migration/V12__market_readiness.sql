ALTER TABLE businesses
    ADD COLUMN gst_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN gstin VARCHAR(15),
    ADD COLUMN state_code VARCHAR(2),
    ADD COLUMN state_name VARCHAR(100),
    ADD COLUMN gst_inclusive_pricing BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN logo_content_type VARCHAR(100),
    ADD COLUMN logo_bytes BYTEA;

ALTER TABLE products
    ADD COLUMN barcode VARCHAR(64),
    ADD COLUMN hsn_code VARCHAR(8),
    ADD COLUMN gst_rate NUMERIC(5, 2) NOT NULL DEFAULT 0;

CREATE UNIQUE INDEX uk_products_business_barcode
    ON products (business_id, lower(barcode))
    WHERE barcode IS NOT NULL AND barcode <> '';

ALTER TABLE sale_items
    ADD COLUMN hsn_code VARCHAR(8),
    ADD COLUMN gst_rate NUMERIC(5, 2) NOT NULL DEFAULT 0,
    ADD COLUMN taxable_amount NUMERIC(19, 2) NOT NULL DEFAULT 0,
    ADD COLUMN cgst_amount NUMERIC(19, 2) NOT NULL DEFAULT 0,
    ADD COLUMN sgst_amount NUMERIC(19, 2) NOT NULL DEFAULT 0,
    ADD COLUMN igst_amount NUMERIC(19, 2) NOT NULL DEFAULT 0;

UPDATE sale_items SET taxable_amount = line_total;

ALTER TABLE sales
    ADD COLUMN interstate BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN taxable_amount NUMERIC(19, 2) NOT NULL DEFAULT 0,
    ADD COLUMN cgst_amount NUMERIC(19, 2) NOT NULL DEFAULT 0,
    ADD COLUMN sgst_amount NUMERIC(19, 2) NOT NULL DEFAULT 0,
    ADD COLUMN igst_amount NUMERIC(19, 2) NOT NULL DEFAULT 0;

UPDATE sales SET taxable_amount = total_amount;

ALTER TABLE purchase_items
    ADD COLUMN hsn_code VARCHAR(8),
    ADD COLUMN gst_rate NUMERIC(5, 2) NOT NULL DEFAULT 0,
    ADD COLUMN taxable_amount NUMERIC(19, 2) NOT NULL DEFAULT 0,
    ADD COLUMN cgst_amount NUMERIC(19, 2) NOT NULL DEFAULT 0,
    ADD COLUMN sgst_amount NUMERIC(19, 2) NOT NULL DEFAULT 0,
    ADD COLUMN igst_amount NUMERIC(19, 2) NOT NULL DEFAULT 0;

UPDATE purchase_items SET taxable_amount = line_total;

ALTER TABLE purchases
    ADD COLUMN interstate BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN taxable_amount NUMERIC(19, 2) NOT NULL DEFAULT 0,
    ADD COLUMN cgst_amount NUMERIC(19, 2) NOT NULL DEFAULT 0,
    ADD COLUMN sgst_amount NUMERIC(19, 2) NOT NULL DEFAULT 0,
    ADD COLUMN igst_amount NUMERIC(19, 2) NOT NULL DEFAULT 0;

UPDATE purchases SET taxable_amount = total_amount;

ALTER TABLE business_memberships
    ADD CONSTRAINT ck_business_memberships_role
        CHECK (role IN ('OWNER', 'MANAGER', 'CLERK'));

CREATE TABLE staff_invites (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    business_id UUID NOT NULL,
    email VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    invited_by UUID NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    accepted_at TIMESTAMPTZ,
    CONSTRAINT fk_staff_invites_business
        FOREIGN KEY (business_id) REFERENCES businesses (id),
    CONSTRAINT fk_staff_invites_invited_by
        FOREIGN KEY (invited_by) REFERENCES app_users (id),
    CONSTRAINT uk_staff_invites_token_hash UNIQUE (token_hash),
    CONSTRAINT ck_staff_invites_role CHECK (role IN ('MANAGER', 'CLERK'))
);

CREATE INDEX idx_staff_invites_business_email ON staff_invites (business_id, email);

CREATE TABLE audit_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    business_id UUID NOT NULL,
    user_id UUID,
    action VARCHAR(80) NOT NULL,
    entity_type VARCHAR(80) NOT NULL,
    entity_id UUID,
    summary VARCHAR(500) NOT NULL,
    CONSTRAINT fk_audit_events_business
        FOREIGN KEY (business_id) REFERENCES businesses (id)
);

CREATE INDEX idx_audit_events_business_created ON audit_events (business_id, created_at DESC);
