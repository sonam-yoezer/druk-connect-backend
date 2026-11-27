-- Preserve existing contact visibility until each member changes their settings.
ALTER TABLE "users" ADD COLUMN "show_email" BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE "users" ADD COLUMN "show_phone_number" BOOLEAN NOT NULL DEFAULT TRUE;
