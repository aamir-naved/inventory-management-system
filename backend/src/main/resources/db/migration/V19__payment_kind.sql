ALTER TABLE payments
    ADD COLUMN payment_kind VARCHAR(20) NOT NULL DEFAULT 'RECEIPT';

ALTER TABLE payments
    ADD CONSTRAINT chk_payments_kind CHECK (payment_kind IN ('RECEIPT', 'REFUND'));
