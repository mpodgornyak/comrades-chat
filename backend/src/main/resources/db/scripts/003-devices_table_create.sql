--liquibase formatted sql
--changeset mikhail:003-devices_table_create

CREATE TABLE devices
(
    id           UUID PRIMARY KEY,
    user_id      UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    device_name  VARCHAR(100),
    device_type  VARCHAR(20) NOT NULL CHECK (device_type IN ('MOBILE', 'WEB', 'DESKTOP')),
    status       VARCHAR(20)              DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'LOGGED_OUT')),
    created_at   TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    last_seen_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_devices_user_id ON devices (user_id) WHERE status = 'ACTIVE';

--rollback DROP TABLE IF EXISTS devices;