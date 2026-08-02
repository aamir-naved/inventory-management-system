ALTER TABLE businesses
    ADD COLUMN allow_negative_stock BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN default_low_stock_threshold NUMERIC(19, 3) NOT NULL DEFAULT 0,
    ADD COLUMN date_format VARCHAR(32) NOT NULL DEFAULT 'dd/MM/yyyy';
