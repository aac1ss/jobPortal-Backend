-- Users table
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    last_login_time TIMESTAMP NULL,
    login_count INTEGER DEFAULT 0,
    password_updated_at TIMESTAMP NULL
);

-- User roles table
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    roles VARCHAR(255) NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, roles)
);

-- Refresh tokens table
CREATE TABLE refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    token VARCHAR(255) NOT NULL UNIQUE,
    expiry_date TIMESTAMP NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Password reset tokens table
CREATE TABLE password_reset_tokens (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(255) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    expiry_date TIMESTAMP NOT NULL,
    used BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Password reset logs table
CREATE TABLE password_reset_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    requested_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP NULL,
    status VARCHAR(50) NOT NULL,
    ip_address VARCHAR(45) NULL,
    user_agent TEXT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
