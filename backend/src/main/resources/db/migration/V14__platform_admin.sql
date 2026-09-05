ALTER TABLE app_users ADD COLUMN platform_role VARCHAR(30);

ALTER TABLE app_users ADD CONSTRAINT chk_app_users_platform_role
    CHECK (platform_role IS NULL OR platform_role = 'PLATFORM_ADMIN');

ALTER TABLE businesses ADD COLUMN suspended_reason VARCHAR(255);

ALTER TABLE businesses ADD COLUMN plan_code VARCHAR(40) NOT NULL DEFAULT 'standard';
