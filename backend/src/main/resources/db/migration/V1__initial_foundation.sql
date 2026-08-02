CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE businesses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    name VARCHAR(150) NOT NULL,
    business_type VARCHAR(100) NOT NULL,
    address_line VARCHAR(255),
    mobile_number VARCHAR(20) NOT NULL,
    currency_code VARCHAR(3) NOT NULL,
    time_zone VARCHAR(50) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE app_users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    full_name VARCHAR(120) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_app_users_email UNIQUE (email)
);

CREATE TABLE business_memberships (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    business_id UUID NOT NULL,
    user_id UUID NOT NULL,
    role VARCHAR(50) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_business_memberships_business
        FOREIGN KEY (business_id) REFERENCES businesses (id),
    CONSTRAINT fk_business_memberships_user
        FOREIGN KEY (user_id) REFERENCES app_users (id),
    CONSTRAINT uk_business_memberships_business_user UNIQUE (business_id, user_id)
);

CREATE INDEX idx_business_memberships_business_id ON business_memberships (business_id);
CREATE INDEX idx_business_memberships_user_id ON business_memberships (user_id);
