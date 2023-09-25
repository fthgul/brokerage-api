-- Drop tables if they exist.
DROP TABLE IF EXISTS public.order_history;
DROP TABLE IF EXISTS public.orders;
DROP TABLE IF EXISTS public.user_stock;
DROP TABLE IF EXISTS public.stocks;
DROP TABLE IF EXISTS public.users;
DROP TYPE IF EXISTS order_type_enum;
DROP TYPE IF EXISTS order_status_enum;
DROP TYPE IF EXISTS process_status_enum;

CREATE TABLE IF NOT EXISTS users
(
    id         SERIAL PRIMARY KEY,
    username   VARCHAR(255) NOT NULL UNIQUE,
    email      VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ  NOT NULL,
    updated_at TIMESTAMPTZ  NOT NULL,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version integer default 0
);

CREATE TABLE IF NOT EXISTS stocks
(
    id         SERIAL PRIMARY KEY,
    ticker   VARCHAR(255) NOT NULL UNIQUE,
    quantity   integer,
    created_at TIMESTAMPTZ  NOT NULL,
    updated_at TIMESTAMPTZ  NOT NULL,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version integer default 0
);

CREATE TYPE order_type_enum AS ENUM ('BUY', 'SELL', 'CANCEL');
CREATE TYPE order_status_enum AS ENUM ('CREATED', 'CANCELLED', 'COMPLETED', 'FAILED');
CREATE TYPE process_status_enum AS ENUM ('PENDING', 'IN_PROGRESS', 'FINALIZED');

CREATE TABLE IF NOT EXISTS order_history
(
    transaction_id     VARCHAR(255) PRIMARY KEY,
    order_id           VARCHAR(255) NOT NULL,
    user_id            BIGINT NOT NULL,
    ticker         VARCHAR(255) NOT NULL,
    order_type         order_type_enum NOT NULL,
    quantity          INTEGER NOT NULL,
    reason             VARCHAR(255),
    created_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
-- Indexes
CREATE INDEX idx_order_history_user_id ON order_history(user_id);
CREATE INDEX idx_order_history_order_id ON order_history(order_id);

CREATE TABLE orders
(
    order_id VARCHAR(255) PRIMARY KEY,
    user_id BIGINT,
    ticker VARCHAR(255),
    order_type order_type_enum NOT NULL ,
    quantity INTEGER,
    status             order_status_enum NOT NULL,
    created_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_order_id ON orders(order_id);
CREATE INDEX idx_user_id ON orders(user_id);
CREATE INDEX idx_ticker ON orders(ticker);
CREATE INDEX idx_status ON orders(status);



CREATE TABLE IF NOT EXISTS user_stock
(
    id          SERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    ticker  VARCHAR(255) NOT NULL,
    quantity    INTEGER NOT NULL DEFAULT 0,
    created_at  TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    created_by  VARCHAR(255),
    updated_by  VARCHAR(255),
    version     INTEGER,
    CONSTRAINT fk_user FOREIGN KEY(user_id) REFERENCES users(id),
    CONSTRAINT unique_user_stock UNIQUE(user_id, ticker)
);
