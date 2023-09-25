TRUNCATE TABLE USERS CASCADE;
INSERT INTO users (username, email, created_at, updated_at, created_by, updated_by, version)
VALUES ('John', 'john@example.com', NOW(), NOW(), 'system', 'system', 0);

truncate table stocks CASCADE;
INSERT INTO stocks (ticker, quantity, created_at, updated_at, created_by, updated_by, version)
VALUES ('APPL', 10, NOW(), NOW(), 'system', 'system', 0);

truncate table user_stock CASCADE;
