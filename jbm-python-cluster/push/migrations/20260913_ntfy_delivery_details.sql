-- Legacy MySQL deployments: run only when push_message_body has neither
-- extend_data nor extend. Restart Push afterwards to reload its column cache.
-- Additive change: keep this nullable column when rolling back the application.
SET SESSION lock_wait_timeout = 5;
ALTER TABLE push_message_body
    ADD COLUMN extend_data LONGTEXT NULL,
    ALGORITHM=INPLACE, LOCK=NONE;
