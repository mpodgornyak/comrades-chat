--liquibase formatted sql
--changeset mikhail:001-initial-schema

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================
-- Пользователи
-- ============================================
CREATE TABLE users
(
    id           UUID PRIMARY KEY,
    username     VARCHAR(50) UNIQUE NOT NULL,
    public_key   TEXT               NOT NULL,
    created_at   TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    last_seen_at TIMESTAMP WITH TIME ZONE
);

-- ============================================
-- Чаты
-- ============================================
CREATE TABLE chats
(
    id         UUID PRIMARY KEY,
    type       VARCHAR(10) NOT NULL CHECK (type IN ('DIRECT', 'GROUP')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- Участники чатов
-- ============================================
CREATE TABLE chat_members
(
    chat_id   UUID REFERENCES chats (id) ON DELETE CASCADE,
    user_id   UUID REFERENCES users (id) ON DELETE CASCADE,
    joined_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (chat_id, user_id)
);

-- ============================================
-- Сообщения: партиционированная структура БЕЗ явных партиций.
-- Все данные пока идут в messages_default.
-- Реальные партиции создаст воркер (задача в backlog перед релизом).
-- ============================================
CREATE TABLE messages
(
    id               UUID                     NOT NULL,
    chat_id          UUID                     NOT NULL,
    sender_id        UUID                     NOT NULL,
    payload          TEXT                     NOT NULL,
    client_timestamp BIGINT                   NOT NULL,
    server_timestamp TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    idempotency_key  UUID                     NOT NULL,
    status           VARCHAR(20)                       DEFAULT 'QUEUED' CHECK (status IN ('QUEUED', 'DELIVERED', 'READ')),
    PRIMARY KEY (id, server_timestamp)
) PARTITION BY RANGE (server_timestamp);

-- Default-партиция: временное хранилище, пока воркер не создаст реальные партиции.
-- ПЕРЕД РЕЛИЗОМ MVP 0.1.0 должна быть очищена (TRUNCATE) после запуска воркера.
CREATE TABLE messages_default PARTITION OF messages DEFAULT;

-- Индексы (автоматически станут партиционированными при создании партиций)
CREATE INDEX idx_messages_chat_id ON messages (chat_id);
CREATE INDEX idx_messages_sender_id ON messages (sender_id);
CREATE INDEX idx_messages_server_timestamp ON messages (server_timestamp);

-- Идемпотентность: защита от дублей при повторной отправке
CREATE UNIQUE INDEX idx_messages_idempotency
    ON messages (sender_id, idempotency_key, server_timestamp);

-- ============================================
-- Статусы доставки
-- ============================================
CREATE TABLE message_receipts
(
    message_id UUID        NOT NULL,
    user_id    UUID REFERENCES users (id) ON DELETE CASCADE,
    status     VARCHAR(10) NOT NULL CHECK (status IN ('DELIVERED', 'READ')),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (message_id, user_id)
);

CREATE INDEX idx_receipts_user_id ON message_receipts (user_id);

--rollback DROP TABLE IF EXISTS message_receipts;
--rollback DROP TABLE IF EXISTS messages;
--rollback DROP TABLE IF EXISTS chat_members;
--rollback DROP TABLE IF EXISTS chats;
--rollback DROP TABLE IF EXISTS users;