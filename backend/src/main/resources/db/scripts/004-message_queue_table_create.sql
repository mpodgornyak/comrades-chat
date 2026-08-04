--liquibase formatted sql
--changeset mikhail:004-message_queue_table_create

CREATE TABLE message_queue
(
    id                  UUID PRIMARY KEY         DEFAULT uuid_generate_v4(),
    message_id          UUID NOT NULL,
    recipient_device_id UUID NOT NULL REFERENCES devices (id) ON DELETE CASCADE,
    created_at          TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    delivered_at        TIMESTAMP WITH TIME ZONE
);

-- Индекс для быстрого поиска недоставленных сообщений по устройству
CREATE INDEX idx_queue_pending ON message_queue (recipient_device_id) WHERE delivered_at IS NULL;

-- Индекс для поиска по message_id
CREATE INDEX idx_queue_message_id ON message_queue (message_id);

--rollback DROP TABLE IF EXISTS message_queue;