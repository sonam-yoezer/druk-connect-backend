-- Align legacy MySQL character columns with the JPA String mappings.
ALTER TABLE "auth_sessions" ALTER COLUMN "current_access_jti" TYPE varchar(36);
ALTER TABLE "auth_sessions" ALTER COLUMN "refresh_token_hash" TYPE varchar(64);
ALTER TABLE "otp_verifications" ALTER COLUMN "otp_hash" TYPE varchar(64);
ALTER TABLE "revoked_access_tokens" ALTER COLUMN "jti" TYPE varchar(36);
ALTER TABLE "vouch_invitations" ALTER COLUMN "token_hash" TYPE varchar(64);
ALTER TABLE "vouch_requests" ALTER COLUMN "response_token_hash" TYPE varchar(64);
