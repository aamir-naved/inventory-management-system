CREATE TABLE phone_otps (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    phone VARCHAR(20) NOT NULL,
    code_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    consumed_at TIMESTAMPTZ,
    attempts INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_phone_otps_phone_created ON phone_otps (phone, created_at DESC);

ALTER TABLE app_users ALTER COLUMN email DROP NOT NULL;
ALTER TABLE app_users ADD COLUMN phone VARCHAR(20);
ALTER TABLE app_users ADD CONSTRAINT uk_app_users_phone UNIQUE (phone);
