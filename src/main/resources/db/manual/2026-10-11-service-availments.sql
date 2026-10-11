CREATE TABLE IF NOT EXISTS listing_service_availments (
    id CHAR(36) NOT NULL,
    listing_id CHAR(36) NOT NULL,
    buyer_user_id CHAR(36) NOT NULL,
    availed_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_service_availment_listing_buyer UNIQUE (listing_id, buyer_user_id),
    KEY ix_service_availment_buyer_date (buyer_user_id, availed_at),
    KEY ix_service_availment_listing_date (listing_id, availed_at),
    CONSTRAINT fk_service_availment_listing FOREIGN KEY (listing_id) REFERENCES listings(id),
    CONSTRAINT fk_service_availment_buyer FOREIGN KEY (buyer_user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
