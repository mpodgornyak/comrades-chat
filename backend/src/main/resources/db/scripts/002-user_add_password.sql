--liquibase formatted sql
--changeset mikhail:002-user_add_password

ALTER TABLE users ADD COLUMN password_hash VARCHAR(100) NOT NULL DEFAULT '';

--rollback ALTER TABLE users DROP COLUMN password_hash;