-- Additive MySQL 8 migration for druk_connect. Existing rows are left unchanged.
CREATE TABLE IF NOT EXISTS listing_review_contacts (
    id CHAR(36) NOT NULL,
    listing_id CHAR(36) NOT NULL,
    buyer_user_id CHAR(36) NOT NULL,
    contacted_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_review_contact_listing_buyer UNIQUE (listing_id, buyer_user_id),
    KEY ix_review_contact_buyer (buyer_user_id),
    CONSTRAINT fk_review_contact_listing FOREIGN KEY (listing_id) REFERENCES listings(id),
    CONSTRAINT fk_review_contact_buyer FOREIGN KEY (buyer_user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS listing_review_requests (
    id CHAR(36) NOT NULL,
    listing_id CHAR(36) NOT NULL,
    buyer_user_id CHAR(36) NOT NULL,
    status VARCHAR(20) NOT NULL,
    requested_at DATETIME(6) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    email_sent_at DATETIME(6) NULL,
    completed_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_review_request_listing_buyer UNIQUE (listing_id, buyer_user_id),
    KEY ix_review_request_buyer (buyer_user_id),
    KEY ix_review_request_listing_requested (listing_id, requested_at),
    CONSTRAINT fk_review_request_listing FOREIGN KEY (listing_id) REFERENCES listings(id),
    CONSTRAINT fk_review_request_buyer FOREIGN KEY (buyer_user_id) REFERENCES users(id),
    CONSTRAINT ck_review_request_status CHECK (status IN ('PENDING', 'COMPLETED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
