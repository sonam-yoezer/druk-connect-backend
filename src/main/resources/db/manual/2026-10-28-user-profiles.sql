ALTER TABLE users
    ADD COLUMN bio VARCHAR(1000) NULL,
    ADD COLUMN location VARCHAR(180) NULL,
    ADD COLUMN profile_image_path VARCHAR(500) NULL;

CREATE TABLE profile_contact_changes (
    id CHAR(36) NOT NULL PRIMARY KEY,
    user_id CHAR(36) NOT NULL,
    channel VARCHAR(10) NOT NULL,
    destination VARCHAR(254) NOT NULL,
    otp_hash VARCHAR(64) NOT NULL,
    attempts INT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    consumed_at DATETIME(6) NULL,
    CONSTRAINT uk_profile_contact_user_channel UNIQUE (user_id, channel),
    CONSTRAINT fk_profile_contact_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
