--liquibase formatted sql
--changeset mikhail:001-users_table_create

CREATE
EXTENSION IF NOT EXISTS "uuid-ossp";


CREATE TABLE users
(
    id           UUID PRIMARY KEY         DEFAULT uuid_generate_v4(),
    username     VARCHAR(50) UNIQUE NOT NULL,
    public_key   TEXT               NOT NULL,
    created_at   TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    last_seen_at TIMESTAMP WITH TIME ZONE
);