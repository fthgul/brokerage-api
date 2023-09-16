CREATE TABLE users
(
    id         SERIAL PRIMARY KEY,
    username   VARCHAR(255) NOT NULL UNIQUE,
    email      VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ  NOT NULL,
    updated_at TIMESTAMPTZ  NOT NULL,
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

INSERT INTO users (id, username, email, created_at, updated_at, created_by, updated_by)
VALUES (999999, 'John', 'john@example.com', NOW(), NOW(), 'system', 'system');

