-- PostgreSQL schema converted from the live MySQL druk_connect database.

SET TIME ZONE 'UTC';

CREATE TABLE "auth_audit_logs" (
  "id" char(36) NOT NULL,
  "event_type" varchar(50) NOT NULL,
  "user_id" char(36),
  "session_id" char(36),
  "target_user_id" char(36),
  "identifier_masked" varchar(254),
  "ip_address" varchar(64),
  "user_agent" varchar(500),
  "metadata_json" json,
  "created_at" timestamp(6) with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE "auth_sessions" (
  "id" char(36) NOT NULL,
  "user_id" char(36) NOT NULL,
  "refresh_token_hash" char(64) NOT NULL,
  "current_access_jti" char(36) NOT NULL,
  "access_expires_at" timestamp(6) with time zone NOT NULL,
  "refresh_expires_at" timestamp(6) with time zone NOT NULL,
  "ip_address" varchar(64),
  "user_agent" varchar(500),
  "issued_at" timestamp(6) with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
  "last_used_at" timestamp(6) with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
  "revoked_at" timestamp(6) with time zone,
  "revocation_reason" varchar(100)
);

CREATE TABLE "community_guideline_acceptances" (
  "id" char(36) NOT NULL,
  "user_id" char(36) NOT NULL,
  "guideline_version" varchar(30) NOT NULL,
  "accepted_at" timestamp(6) with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
  "ip_address" varchar(64),
  "user_agent" varchar(500)
);

CREATE TABLE "lister_posting_restrictions" (
  "id" char(36) NOT NULL,
  "lister_user_id" char(36) NOT NULL,
  "source_report_id" char(36) NOT NULL,
  "active" boolean DEFAULT TRUE NOT NULL,
  "reason" varchar(1000) NOT NULL,
  "restricted_at" timestamp(6) with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
  "cleared_at" timestamp(6) with time zone,
  "cleared_by_user_id" char(36),
  "clearance_reason" varchar(1000),
  "version" bigint DEFAULT '0' NOT NULL
);

CREATE TABLE "lister_scam_appeals" (
  "id" char(36) NOT NULL,
  "lister_user_id" char(36) NOT NULL,
  "restriction_id" char(36) NOT NULL,
  "source_report_id" char(36) NOT NULL,
  "appeal_reason" varchar(2000) NOT NULL,
  "status" varchar(20) DEFAULT 'PENDING' NOT NULL,
  "submitted_at" timestamp(6) with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
  "moderated_by_user_id" char(36),
  "moderated_at" timestamp(6) with time zone,
  "moderation_reason" varchar(1000)
);

CREATE TABLE "lister_scam_reports" (
  "id" char(36) NOT NULL,
  "reported_lister_id" char(36) NOT NULL,
  "reporter_user_id" char(36) NOT NULL,
  "source_listing_id" char(36) NOT NULL,
  "source_listing_title" varchar(255) NOT NULL,
  "reason" varchar(500) NOT NULL,
  "details" varchar(2000),
  "status" varchar(20) DEFAULT 'PENDING' NOT NULL,
  "reported_at" timestamp(6) with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
  "moderated_by_user_id" char(36),
  "moderated_at" timestamp(6) with time zone,
  "moderation_reason" varchar(1000)
);

CREATE TABLE "listing_dietary_options" (
  "listing_id" char(36) NOT NULL,
  "dietary_option" varchar(100) NOT NULL
);

CREATE TABLE "listing_images" (
  "id" char(36) NOT NULL,
  "listing_id" char(36) NOT NULL,
  "image_path" varchar(500) NOT NULL,
  "original_filename" varchar(255),
  "content_type" varchar(100) NOT NULL,
  "sort_order" int NOT NULL,
  "created_at" timestamp(6) with time zone NOT NULL
);

CREATE TABLE "listing_review_contacts" (
  "id" char(36) NOT NULL,
  "listing_id" char(36) NOT NULL,
  "buyer_user_id" char(36) NOT NULL,
  "contacted_at" timestamp(6) with time zone NOT NULL
);

CREATE TABLE "listing_review_requests" (
  "id" char(36) NOT NULL,
  "listing_id" char(36) NOT NULL,
  "buyer_user_id" char(36) NOT NULL,
  "status" varchar(20) NOT NULL,
  "requested_at" timestamp(6) with time zone NOT NULL,
  "expires_at" timestamp(6) with time zone NOT NULL,
  "email_sent_at" timestamp(6) with time zone,
  "completed_at" timestamp(6) with time zone
);

CREATE TABLE "listing_reviews" (
  "id" char(36) NOT NULL,
  "listing_id" char(36) NOT NULL,
  "reviewer_user_id" char(36) NOT NULL,
  "rating" int NOT NULL,
  "comment_text" varchar(2000),
  "status" varchar(20) DEFAULT 'PENDING' NOT NULL,
  "submitted_at" timestamp(6) with time zone NOT NULL,
  "moderated_at" timestamp(6) with time zone,
  "moderated_by_user_id" char(36),
  "rejection_reason" varchar(500)
);

CREATE TABLE "listing_service_availments" (
  "id" char(36) NOT NULL,
  "listing_id" char(36) NOT NULL,
  "buyer_user_id" char(36) NOT NULL,
  "availed_at" timestamp(6) with time zone NOT NULL
);

CREATE TABLE "listings" (
  "id" char(36) NOT NULL,
  "lister_user_id" char(36) NOT NULL,
  "listing_title" varchar(180) NOT NULL,
  "listing_category" varchar(120) NOT NULL,
  "suburb" varchar(120) NOT NULL,
  "state" varchar(100) NOT NULL,
  "postcode" varchar(20) NOT NULL,
  "description" text NOT NULL,
  "service_type" varchar(120) NOT NULL,
  "availability" varchar(100) NOT NULL,
  "pricing_type" varchar(20) NOT NULL,
  "rate_amount" decimal(10,2),
  "currency_code" varchar(3) DEFAULT 'AUD' NOT NULL,
  "views" bigint DEFAULT '0' NOT NULL,
  "status" varchar(20) DEFAULT 'ACTIVE' NOT NULL,
  "created_at" timestamp(6) with time zone NOT NULL,
  "updated_at" timestamp(6) with time zone NOT NULL
);

CREATE TABLE "otp_verifications" (
  "id" char(36) NOT NULL,
  "user_id" char(36) NOT NULL,
  "channel" varchar(20) NOT NULL,
  "purpose" varchar(30) NOT NULL,
  "destination" varchar(254) NOT NULL,
  "otp_hash" char(64) NOT NULL,
  "expires_at" timestamp(6) with time zone NOT NULL,
  "verified_at" timestamp(6) with time zone,
  "invalidated_at" timestamp(6) with time zone,
  "attempt_count" int DEFAULT '0' NOT NULL,
  "max_attempts" int DEFAULT '5' NOT NULL,
  "delivery_status" varchar(20) DEFAULT 'PENDING' NOT NULL,
  "delivery_error" varchar(500),
  "sent_at" timestamp(6) with time zone,
  "requested_ip" varchar(64),
  "created_at" timestamp(6) with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE "platform_feedback" (
  "id" char(36) NOT NULL,
  "source" varchar(20) NOT NULL,
  "feedback_type" varchar(30) NOT NULL,
  "perspective" varchar(30) NOT NULL,
  "micro_rating" varchar(20),
  "message" text,
  "anonymous" boolean DEFAULT FALSE NOT NULL,
  "submitted_by_user_id" char(36),
  "submitter_name_snapshot" varchar(200),
  "submitter_email_snapshot" varchar(255),
  "submitter_access_type" varchar(30),
  "screenshot_path" varchar(1000),
  "screenshot_original_filename" varchar(255),
  "screenshot_content_type" varchar(100),
  "status" varchar(30) DEFAULT 'NEW' NOT NULL,
  "theme_key" varchar(100),
  "theme_label" varchar(300),
  "status_updated_by_user_id" char(36),
  "submitted_at" timestamp(6) with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
  "updated_at" timestamp(6) with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
  "resolved_at" timestamp(6) with time zone
);

CREATE TABLE "profile_contact_changes" (
  "id" char(36) NOT NULL,
  "user_id" char(36) NOT NULL,
  "channel" varchar(10) NOT NULL,
  "destination" varchar(254) NOT NULL,
  "otp_hash" varchar(64) NOT NULL,
  "attempts" int NOT NULL,
  "created_at" timestamp(6) with time zone NOT NULL,
  "expires_at" timestamp(6) with time zone NOT NULL,
  "consumed_at" timestamp(6) with time zone
);

CREATE TABLE "review_tags" (
  "review_id" char(36) NOT NULL,
  "tag" varchar(80) NOT NULL
);

CREATE TABLE "revoked_access_tokens" (
  "id" char(36) NOT NULL,
  "jti" char(36) NOT NULL,
  "session_id" char(36) NOT NULL,
  "user_id" char(36) NOT NULL,
  "expires_at" timestamp(6) with time zone NOT NULL,
  "revoked_at" timestamp(6) with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
  "reason" varchar(100) NOT NULL
);

CREATE TABLE "roles" (
  "id" bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
  "code" varchar(50) NOT NULL,
  "name" varchar(100) NOT NULL,
  "description" varchar(255),
  "is_active" boolean DEFAULT TRUE NOT NULL
);

CREATE TABLE "user_roles" (
  "id" bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
  "user_id" char(36) NOT NULL,
  "role_id" bigint NOT NULL,
  "assigned_at" timestamp(6) with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
  "assigned_by" char(36)
);

CREATE TABLE "users" (
  "id" char(36) NOT NULL,
  "first_name" varchar(100) NOT NULL,
  "last_name" varchar(100) NOT NULL,
  "email" varchar(254) NOT NULL,
  "phone_number" varchar(20) NOT NULL,
  "access_type" varchar(20) DEFAULT 'BUYER' NOT NULL,
  "password_hash" varchar(255) NOT NULL,
  "status" varchar(32) DEFAULT 'PENDING_VERIFICATION' NOT NULL,
  "phone_verified_at" timestamp(6) with time zone,
  "email_verified_at" timestamp(6) with time zone,
  "failed_login_attempts" int DEFAULT '0' NOT NULL,
  "last_login_at" timestamp(6) with time zone,
  "created_at" timestamp(6) with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
  "updated_at" timestamp(6) with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
  "bio" varchar(1000),
  "location" varchar(180),
  "profile_image_path" varchar(500)
);

CREATE TABLE "vouch_invitations" (
  "id" char(36) NOT NULL,
  "requester_user_id" char(36) NOT NULL,
  "invitee_email" varchar(254) NOT NULL,
  "message" varchar(500),
  "token_hash" char(64) NOT NULL,
  "status" varchar(20) DEFAULT 'PENDING' NOT NULL,
  "registered_user_id" char(36),
  "vouch_request_id" char(36),
  "expires_at" timestamp(6) with time zone NOT NULL,
  "sent_at" timestamp(6) with time zone,
  "accepted_at" timestamp(6) with time zone,
  "cancelled_at" timestamp(6) with time zone,
  "created_at" timestamp(6) with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE "vouch_requests" (
  "id" char(36) NOT NULL,
  "requester_user_id" char(36) NOT NULL,
  "target_user_id" char(36) NOT NULL,
  "message" varchar(500) NOT NULL,
  "status" varchar(20) DEFAULT 'PENDING' NOT NULL,
  "requested_at" timestamp(6) with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
  "responded_at" timestamp(6) with time zone,
  "cancelled_at" timestamp(6) with time zone,
  "response_token_hash" char(64),
  "response_token_expires_at" timestamp(6) with time zone,
  "notification_sent_at" timestamp(6) with time zone
);

CREATE TABLE "vouch_withdrawal_requests" (
  "id" char(36) NOT NULL,
  "vouch_id" char(36) NOT NULL,
  "requested_by_user_id" char(36) NOT NULL,
  "reason" varchar(500) NOT NULL,
  "status" varchar(30) NOT NULL,
  "requested_at" timestamp(6) with time zone NOT NULL,
  "moderated_at" timestamp(6) with time zone,
  "moderated_by_user_id" char(36),
  "admin_reason" varchar(500)
);

CREATE TABLE "vouches" (
  "id" char(36) NOT NULL,
  "voucher_user_id" char(36) NOT NULL,
  "vouched_user_id" char(36) NOT NULL,
  "vouch_request_id" char(36) NOT NULL,
  "status" varchar(20) DEFAULT 'ACTIVE' NOT NULL,
  "vouched_at" timestamp(6) with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
  "revoked_at" timestamp(6) with time zone
);

ALTER TABLE "auth_audit_logs" ADD PRIMARY KEY ("id");

CREATE INDEX "idx_auth_audit_event_created" ON "auth_audit_logs" ("event_type", "created_at");

CREATE INDEX "idx_auth_audit_session_created" ON "auth_audit_logs" ("session_id", "created_at");

CREATE INDEX "idx_auth_audit_target_created" ON "auth_audit_logs" ("target_user_id", "created_at");

CREATE INDEX "idx_auth_audit_user_created" ON "auth_audit_logs" ("user_id", "created_at");

ALTER TABLE "auth_sessions" ADD PRIMARY KEY ("id");

CREATE INDEX "idx_auth_session_refresh_expiry" ON "auth_sessions" ("refresh_expires_at");

CREATE INDEX "idx_auth_session_revoked" ON "auth_sessions" ("revoked_at");

CREATE INDEX "idx_auth_session_user" ON "auth_sessions" ("user_id");

CREATE UNIQUE INDEX "uk_auth_session_refresh_hash" ON "auth_sessions" ("refresh_token_hash");

ALTER TABLE "community_guideline_acceptances" ADD PRIMARY KEY ("id");

CREATE INDEX "idx_guideline_user" ON "community_guideline_acceptances" ("user_id");

CREATE UNIQUE INDEX "uk_guideline_user_version" ON "community_guideline_acceptances" ("user_id", "guideline_version");

ALTER TABLE "lister_posting_restrictions" ADD PRIMARY KEY ("id");

CREATE INDEX "fk_lister_restriction_cleared_by" ON "lister_posting_restrictions" ("cleared_by_user_id");

CREATE INDEX "idx_lister_posting_restriction_active" ON "lister_posting_restrictions" ("lister_user_id", "active");

CREATE INDEX "idx_lister_posting_restriction_report" ON "lister_posting_restrictions" ("source_report_id");

CREATE UNIQUE INDEX "uk_lister_posting_restriction_user" ON "lister_posting_restrictions" ("lister_user_id");

ALTER TABLE "lister_scam_appeals" ADD PRIMARY KEY ("id");

CREATE INDEX "fk_scam_appeal_moderator" ON "lister_scam_appeals" ("moderated_by_user_id");

CREATE INDEX "idx_lister_scam_appeal_report" ON "lister_scam_appeals" ("source_report_id");

CREATE INDEX "idx_lister_scam_appeal_restriction" ON "lister_scam_appeals" ("restriction_id");

CREATE INDEX "idx_lister_scam_appeal_status" ON "lister_scam_appeals" ("status", "submitted_at");

CREATE INDEX "idx_lister_scam_appeal_user" ON "lister_scam_appeals" ("lister_user_id", "status");

ALTER TABLE "lister_scam_reports" ADD PRIMARY KEY ("id");

CREATE INDEX "fk_scam_report_moderator" ON "lister_scam_reports" ("moderated_by_user_id");

CREATE INDEX "idx_lister_scam_report_lister" ON "lister_scam_reports" ("reported_lister_id", "status");

CREATE INDEX "idx_lister_scam_report_listing" ON "lister_scam_reports" ("source_listing_id");

CREATE INDEX "idx_lister_scam_report_reporter" ON "lister_scam_reports" ("reporter_user_id");

CREATE INDEX "idx_lister_scam_report_status" ON "lister_scam_reports" ("status", "reported_at");

ALTER TABLE "listing_dietary_options" ADD PRIMARY KEY ("listing_id", "dietary_option");

ALTER TABLE "listing_images" ADD PRIMARY KEY ("id");

CREATE INDEX "idx_listing_images_listing" ON "listing_images" ("listing_id");

ALTER TABLE "listing_review_contacts" ADD PRIMARY KEY ("id");

CREATE INDEX "ix_review_contact_buyer" ON "listing_review_contacts" ("buyer_user_id");

CREATE UNIQUE INDEX "uk_review_contact_listing_buyer" ON "listing_review_contacts" ("listing_id", "buyer_user_id");

ALTER TABLE "listing_review_requests" ADD PRIMARY KEY ("id");

CREATE INDEX "ix_review_request_buyer" ON "listing_review_requests" ("buyer_user_id");

CREATE INDEX "ix_review_request_listing_requested" ON "listing_review_requests" ("listing_id", "requested_at");

CREATE UNIQUE INDEX "uk_review_request_listing_buyer" ON "listing_review_requests" ("listing_id", "buyer_user_id");

ALTER TABLE "listing_reviews" ADD PRIMARY KEY ("id");

CREATE INDEX "fk_review_moderator" ON "listing_reviews" ("moderated_by_user_id");

CREATE INDEX "idx_review_listing_status" ON "listing_reviews" ("listing_id", "status");

CREATE INDEX "idx_review_user" ON "listing_reviews" ("reviewer_user_id");

ALTER TABLE "listing_service_availments" ADD PRIMARY KEY ("id");

CREATE INDEX "ix_service_availment_buyer_date" ON "listing_service_availments" ("buyer_user_id", "availed_at");

CREATE INDEX "ix_service_availment_listing_date" ON "listing_service_availments" ("listing_id", "availed_at");

CREATE UNIQUE INDEX "uk_service_availment_listing_buyer" ON "listing_service_availments" ("listing_id", "buyer_user_id");

ALTER TABLE "listings" ADD PRIMARY KEY ("id");

CREATE INDEX "idx_listing_lister" ON "listings" ("lister_user_id");

CREATE INDEX "idx_listing_postcode" ON "listings" ("postcode");

CREATE INDEX "idx_listing_state" ON "listings" ("state");

CREATE INDEX "idx_listing_status_created" ON "listings" ("status", "created_at");

CREATE INDEX "idx_listing_suburb" ON "listings" ("suburb");

ALTER TABLE "otp_verifications" ADD PRIMARY KEY ("id");

CREATE INDEX "idx_otp_expiry" ON "otp_verifications" ("expires_at");

CREATE INDEX "idx_otp_lookup" ON "otp_verifications" ("user_id", "channel", "purpose", "created_at");

ALTER TABLE "platform_feedback" ADD PRIMARY KEY ("id");

CREATE INDEX "idx_feedback_source" ON "platform_feedback" ("source");

CREATE INDEX "idx_feedback_status" ON "platform_feedback" ("status", "submitted_at");

CREATE INDEX "idx_feedback_status_admin" ON "platform_feedback" ("status_updated_by_user_id");

CREATE INDEX "idx_feedback_submitter" ON "platform_feedback" ("submitted_by_user_id");

CREATE INDEX "idx_feedback_submitter_type" ON "platform_feedback" ("submitter_access_type");

CREATE INDEX "idx_feedback_theme" ON "platform_feedback" ("theme_key");

CREATE INDEX "idx_feedback_type" ON "platform_feedback" ("feedback_type");

ALTER TABLE "profile_contact_changes" ADD PRIMARY KEY ("id");

CREATE UNIQUE INDEX "uk_profile_contact_user_channel" ON "profile_contact_changes" ("user_id", "channel");

ALTER TABLE "review_tags" ADD PRIMARY KEY ("review_id", "tag");

ALTER TABLE "revoked_access_tokens" ADD PRIMARY KEY ("id");

CREATE INDEX "idx_revoked_access_expiry" ON "revoked_access_tokens" ("expires_at");

CREATE INDEX "idx_revoked_access_session" ON "revoked_access_tokens" ("session_id");

CREATE INDEX "idx_revoked_access_user" ON "revoked_access_tokens" ("user_id");

CREATE UNIQUE INDEX "uk_revoked_access_jti" ON "revoked_access_tokens" ("jti");

ALTER TABLE "roles" ADD PRIMARY KEY ("id");

CREATE UNIQUE INDEX "uk_roles_code" ON "roles" ("code");

ALTER TABLE "user_roles" ADD PRIMARY KEY ("id");

CREATE INDEX "idx_user_roles_assigned_by" ON "user_roles" ("assigned_by");

CREATE INDEX "idx_user_roles_role" ON "user_roles" ("role_id");

CREATE INDEX "idx_user_roles_user" ON "user_roles" ("user_id");

CREATE UNIQUE INDEX "uk_user_role" ON "user_roles" ("user_id", "role_id");

ALTER TABLE "users" ADD PRIMARY KEY ("id");

CREATE UNIQUE INDEX "uk_users_email" ON "users" ("email");

CREATE UNIQUE INDEX "uk_users_phone" ON "users" ("phone_number");

ALTER TABLE "vouch_invitations" ADD PRIMARY KEY ("id");

CREATE INDEX "fk_vouch_invitation_request" ON "vouch_invitations" ("vouch_request_id");

CREATE INDEX "idx_vouch_invitation_email" ON "vouch_invitations" ("invitee_email", "status");

CREATE INDEX "idx_vouch_invitation_expiry" ON "vouch_invitations" ("expires_at");

CREATE INDEX "idx_vouch_invitation_registered" ON "vouch_invitations" ("registered_user_id", "status");

CREATE INDEX "idx_vouch_invitation_requester" ON "vouch_invitations" ("requester_user_id", "status");

CREATE UNIQUE INDEX "uk_vouch_invitation_token" ON "vouch_invitations" ("token_hash");

ALTER TABLE "vouch_requests" ADD PRIMARY KEY ("id");

CREATE INDEX "idx_vouch_request_requester" ON "vouch_requests" ("requester_user_id", "status", "requested_at");

CREATE INDEX "idx_vouch_request_target" ON "vouch_requests" ("target_user_id", "status", "requested_at");

CREATE UNIQUE INDEX "uk_vouch_request_response_token" ON "vouch_requests" ("response_token_hash");

ALTER TABLE "vouch_withdrawal_requests" ADD PRIMARY KEY ("id");

CREATE INDEX "fk_vouch_withdrawal_admin" ON "vouch_withdrawal_requests" ("moderated_by_user_id");

CREATE INDEX "idx_vouch_withdrawal_requester" ON "vouch_withdrawal_requests" ("requested_by_user_id");

CREATE INDEX "idx_vouch_withdrawal_status" ON "vouch_withdrawal_requests" ("status");

CREATE INDEX "idx_vouch_withdrawal_vouch" ON "vouch_withdrawal_requests" ("vouch_id");

ALTER TABLE "vouches" ADD PRIMARY KEY ("id");

CREATE INDEX "idx_vouches_vouched_user" ON "vouches" ("vouched_user_id", "status");

CREATE INDEX "idx_vouches_voucher_user" ON "vouches" ("voucher_user_id", "status");

CREATE UNIQUE INDEX "uk_vouch_request" ON "vouches" ("vouch_request_id");

CREATE UNIQUE INDEX "uk_voucher_vouched" ON "vouches" ("voucher_user_id", "vouched_user_id");

ALTER TABLE "auth_sessions" ADD CONSTRAINT "fk_auth_session_user" FOREIGN KEY ("user_id") REFERENCES "users" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE "community_guideline_acceptances" ADD CONSTRAINT "fk_guideline_user" FOREIGN KEY ("user_id") REFERENCES "users" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE "lister_posting_restrictions" ADD CONSTRAINT "fk_lister_restriction_cleared_by" FOREIGN KEY ("cleared_by_user_id") REFERENCES "users" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE "lister_posting_restrictions" ADD CONSTRAINT "fk_lister_restriction_report" FOREIGN KEY ("source_report_id") REFERENCES "lister_scam_reports" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE "lister_posting_restrictions" ADD CONSTRAINT "fk_lister_restriction_user" FOREIGN KEY ("lister_user_id") REFERENCES "users" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE "listing_dietary_options" ADD CONSTRAINT "fk_listing_dietary_listing" FOREIGN KEY ("listing_id") REFERENCES "listings" ("id") ON UPDATE NO ACTION ON DELETE CASCADE;

ALTER TABLE "listing_images" ADD CONSTRAINT "fk_listing_image_listing" FOREIGN KEY ("listing_id") REFERENCES "listings" ("id") ON UPDATE NO ACTION ON DELETE CASCADE;

ALTER TABLE "listings" ADD CONSTRAINT "fk_listing_lister" FOREIGN KEY ("lister_user_id") REFERENCES "users" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE "otp_verifications" ADD CONSTRAINT "fk_otp_user" FOREIGN KEY ("user_id") REFERENCES "users" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE "platform_feedback" ADD CONSTRAINT "fk_platform_feedback_status_admin" FOREIGN KEY ("status_updated_by_user_id") REFERENCES "users" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE "platform_feedback" ADD CONSTRAINT "fk_platform_feedback_submitter" FOREIGN KEY ("submitted_by_user_id") REFERENCES "users" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE "profile_contact_changes" ADD CONSTRAINT "fk_profile_contact_user" FOREIGN KEY ("user_id") REFERENCES "users" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE "listing_review_contacts" ADD CONSTRAINT "fk_review_contact_buyer" FOREIGN KEY ("buyer_user_id") REFERENCES "users" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE "listing_review_contacts" ADD CONSTRAINT "fk_review_contact_listing" FOREIGN KEY ("listing_id") REFERENCES "listings" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE "listing_reviews" ADD CONSTRAINT "fk_review_listing" FOREIGN KEY ("listing_id") REFERENCES "listings" ("id") ON UPDATE NO ACTION ON DELETE CASCADE;

ALTER TABLE "listing_reviews" ADD CONSTRAINT "fk_review_moderator" FOREIGN KEY ("moderated_by_user_id") REFERENCES "users" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE "listing_review_requests" ADD CONSTRAINT "fk_review_request_buyer" FOREIGN KEY ("buyer_user_id") REFERENCES "users" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE "listing_review_requests" ADD CONSTRAINT "fk_review_request_listing" FOREIGN KEY ("listing_id") REFERENCES "listings" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE "review_tags" ADD CONSTRAINT "fk_review_tag_review" FOREIGN KEY ("review_id") REFERENCES "listing_reviews" ("id") ON UPDATE NO ACTION ON DELETE CASCADE;

ALTER TABLE "listing_reviews" ADD CONSTRAINT "fk_review_user" FOREIGN KEY ("reviewer_user_id") REFERENCES "users" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE "revoked_access_tokens" ADD CONSTRAINT "fk_revoked_access_session" FOREIGN KEY ("session_id") REFERENCES "auth_sessions" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE "revoked_access_tokens" ADD CONSTRAINT "fk_revoked_access_user" FOREIGN KEY ("user_id") REFERENCES "users" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE "lister_scam_appeals" ADD CONSTRAINT "fk_scam_appeal_lister" FOREIGN KEY ("lister_user_id") REFERENCES "users" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE "lister_scam_appeals" ADD CONSTRAINT "fk_scam_appeal_moderator" FOREIGN KEY ("moderated_by_user_id") REFERENCES "users" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE "lister_scam_appeals" ADD CONSTRAINT "fk_scam_appeal_report" FOREIGN KEY ("source_report_id") REFERENCES "lister_scam_reports" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE "lister_scam_appeals" ADD CONSTRAINT "fk_scam_appeal_restriction" FOREIGN KEY ("restriction_id") REFERENCES "lister_posting_restrictions" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE "lister_scam_reports" ADD CONSTRAINT "fk_scam_report_lister" FOREIGN KEY ("reported_lister_id") REFERENCES "users" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE "lister_scam_reports" ADD CONSTRAINT "fk_scam_report_moderator" FOREIGN KEY ("moderated_by_user_id") REFERENCES "users" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE "lister_scam_reports" ADD CONSTRAINT "fk_scam_report_reporter" FOREIGN KEY ("reporter_user_id") REFERENCES "users" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE "listing_service_availments" ADD CONSTRAINT "fk_service_availment_buyer" FOREIGN KEY ("buyer_user_id") REFERENCES "users" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE "listing_service_availments" ADD CONSTRAINT "fk_service_availment_listing" FOREIGN KEY ("listing_id") REFERENCES "listings" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE "user_roles" ADD CONSTRAINT "fk_user_roles_assigned_by" FOREIGN KEY ("assigned_by") REFERENCES "users" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE "user_roles" ADD CONSTRAINT "fk_user_roles_role" FOREIGN KEY ("role_id") REFERENCES "roles" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE "user_roles" ADD CONSTRAINT "fk_user_roles_user" FOREIGN KEY ("user_id") REFERENCES "users" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE "vouch_invitations" ADD CONSTRAINT "fk_vouch_invitation_registered_user" FOREIGN KEY ("registered_user_id") REFERENCES "users" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE "vouch_invitations" ADD CONSTRAINT "fk_vouch_invitation_request" FOREIGN KEY ("vouch_request_id") REFERENCES "vouch_requests" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE "vouch_invitations" ADD CONSTRAINT "fk_vouch_invitation_requester" FOREIGN KEY ("requester_user_id") REFERENCES "users" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE "vouches" ADD CONSTRAINT "fk_vouch_request" FOREIGN KEY ("vouch_request_id") REFERENCES "vouch_requests" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE "vouch_requests" ADD CONSTRAINT "fk_vouch_request_requester" FOREIGN KEY ("requester_user_id") REFERENCES "users" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE "vouch_requests" ADD CONSTRAINT "fk_vouch_request_target" FOREIGN KEY ("target_user_id") REFERENCES "users" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE "vouches" ADD CONSTRAINT "fk_vouch_vouched" FOREIGN KEY ("vouched_user_id") REFERENCES "users" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE "vouches" ADD CONSTRAINT "fk_vouch_voucher" FOREIGN KEY ("voucher_user_id") REFERENCES "users" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE "vouch_withdrawal_requests" ADD CONSTRAINT "fk_vouch_withdrawal_admin" FOREIGN KEY ("moderated_by_user_id") REFERENCES "users" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE "vouch_withdrawal_requests" ADD CONSTRAINT "fk_vouch_withdrawal_requester" FOREIGN KEY ("requested_by_user_id") REFERENCES "users" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE "vouch_withdrawal_requests" ADD CONSTRAINT "fk_vouch_withdrawal_vouch" FOREIGN KEY ("vouch_id") REFERENCES "vouches" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE "listing_review_requests" ADD CONSTRAINT "ck_review_request_status" CHECK (("status" in ('PENDING','COMPLETED')));

ALTER TABLE "listing_reviews" ADD CONSTRAINT "chk_review_rating" CHECK (("rating" between 1 and 5));

ALTER TABLE "listing_reviews" ADD CONSTRAINT "chk_review_status" CHECK (("status" in ('PENDING','APPROVED','REJECTED')));

ALTER TABLE "listings" ADD CONSTRAINT "chk_listing_availability" CHECK (regexp_like("availability",'^(MONDAY|TUESDAY|WEDNESDAY|THURSDAY|FRIDAY|SATURDAY|SUNDAY)(,(MONDAY|TUESDAY|WEDNESDAY|THURSDAY|FRIDAY|SATURDAY|SUNDAY))*$','c'));

ALTER TABLE "listings" ADD CONSTRAINT "chk_listing_pricing" CHECK (("pricing_type" in ('PAID','FREE')));

ALTER TABLE "listings" ADD CONSTRAINT "chk_listing_status" CHECK (("status" in ('ACTIVE','INACTIVE','DELETED')));

ALTER TABLE "otp_verifications" ADD CONSTRAINT "chk_otp_attempts" CHECK ((("attempt_count" >= 0) and ("max_attempts" > 0)));

ALTER TABLE "platform_feedback" ADD CONSTRAINT "chk_feedback_micro_rating" CHECK ((("micro_rating" is null) or ("micro_rating" in ('EASY','CONFUSING'))));

ALTER TABLE "platform_feedback" ADD CONSTRAINT "chk_feedback_perspective" CHECK (("perspective" in ('BROWSING_USER','LISTER','BOTH')));

ALTER TABLE "platform_feedback" ADD CONSTRAINT "chk_feedback_source" CHECK (("source" in ('MICRO','FORM')));

ALTER TABLE "platform_feedback" ADD CONSTRAINT "chk_feedback_source_type" CHECK (((("source" = 'MICRO') and ("feedback_type" = 'MICRO') and ("micro_rating" is not null)) or (("source" = 'FORM') and ("feedback_type" <> 'MICRO') and ("micro_rating" is null))));

ALTER TABLE "platform_feedback" ADD CONSTRAINT "chk_feedback_status" CHECK (("status" in ('NEW','IN_PROGRESS','RESOLVED')));

ALTER TABLE "platform_feedback" ADD CONSTRAINT "chk_feedback_type" CHECK (("feedback_type" in ('MICRO','BUG','SUGGESTION','COMPLAINT','OTHER')));

ALTER TABLE "users" ADD CONSTRAINT "chk_users_access_type" CHECK (("access_type" in ('BUYER','LISTER')));

ALTER TABLE "users" ADD CONSTRAINT "chk_users_failed_login" CHECK (("failed_login_attempts" >= 0));

ALTER TABLE "vouch_requests" ADD CONSTRAINT "chk_vouch_request_not_self" CHECK (("requester_user_id" <> "target_user_id"));

ALTER TABLE "vouch_withdrawal_requests" ADD CONSTRAINT "chk_vouch_withdrawal_status" CHECK (("status" in ('PENDING','APPROVED','REJECTED')));

ALTER TABLE "vouches" ADD CONSTRAINT "chk_vouch_not_self" CHECK (("voucher_user_id" <> "vouched_user_id"));
