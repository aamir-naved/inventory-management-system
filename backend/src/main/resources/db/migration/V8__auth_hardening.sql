CREATE TABLE auth_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    type VARCHAR(40) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_auth_tokens_user
        FOREIGN KEY (user_id) REFERENCES app_users (id),
    CONSTRAINT uk_auth_tokens_token_hash UNIQUE (token_hash),
    CONSTRAINT ck_auth_tokens_type
        CHECK (type IN ('EMAIL_VERIFY', 'PASSWORD_RESET', 'REFRESH'))
);

CREATE INDEX idx_auth_tokens_user_id_type ON auth_tokens (user_id, type);
CREATE INDEX idx_auth_tokens_expires_at ON auth_tokens (expires_at);
