# User profiles and profile images

Implemented for Buyer, Lister, and Admin accounts. Frontend profile pages now use the real account data instead of placeholder names, photos, and contact details.

## Endpoints

Base URL: `http://localhost:8080`.

| Method | Path | Authentication | Purpose |
|---|---|---|---|
| GET | `/api/v1/users/me/profile` | Normal Bearer access token | Read own profile. |
| PATCH | `/api/v1/users/me/profile` | Normal Bearer access token | Save names, bio, and location. |
| PUT | `/api/v1/users/me/profile-image` | Normal Bearer access token | Upload or replace profile photo. |
| DELETE | `/api/v1/users/me/profile-image` | Normal Bearer access token | Remove photo and return to initials. |
| GET | `/api/v1/users/{userId}/profile-image?v=<version>` | Public | Display an active user's avatar. Use the URL returned by the API. |
| POST | `/api/v1/users/me/contact-changes` | Normal Bearer access token | Confirm current password and send a code to the new contact. |
| POST | `/api/v1/users/me/contact-changes/verify` | Normal Bearer access token | Verify the code and save the new contact. |

All editing endpoints determine the account from the JWT. Clients cannot select another user's ID, change roles, or change access type through profile editing. Public image access exposes only the re-encoded avatar, not the account profile or filesystem path.

### Profile response example

```json
{
  "id": "<userId>",
  "firstName": "Sonam",
  "lastName": "Yoezer",
  "email": "sonam@example.com",
  "phoneNumber": "+61412345678",
  "accessType": "LISTER",
  "roles": ["ENDUSER"],
  "bio": "Community member offering local services.",
  "location": "Melbourne, VIC",
  "profileImageUrl": "/api/v1/users/<userId>/profile-image?v=<photo-version>.png",
  "emailVerifiedAt": "<UTC timestamp or null>",
  "phoneVerifiedAt": "<UTC timestamp or null>"
}
```

`profileImageUrl` is null when no photo is set. The frontend shared avatar displays initials if the URL is absent or the image cannot be loaded. Relative image URLs use `NEXT_PUBLIC_BACKEND_URL`.

### Update details

```http
PATCH /api/v1/users/me/profile
Authorization: Bearer <accessToken>
Content-Type: application/json
```

```json
{
  "firstName": "Sonam",
  "lastName": "Yoezer",
  "bio": "Community member offering local services.",
  "location": "Melbourne, VIC"
}
```

First/last names are required, at most 100 characters each; bio is at most 1000 characters; location is at most 180 characters. Bio/location can be cleared. Email and phone are updated through the verification endpoints below.

### Upload or replace a photo

```http
PUT /api/v1/users/me/profile-image
Authorization: Bearer <accessToken>
Content-Type: multipart/form-data; boundary=<client-generated>
```

File part name: `image`.

JPEG or PNG, up to 5 MB and 16 megapixels. The backend checks the decoded image rather than trusting its filename or declared MIME type, resizes to a maximum edge of 1024 pixels, and writes a new PNG without uploaded EXIF/GPS metadata. It generates the filename itself.

The response is the updated profile. Replaced files are removed only after the database transaction commits; a rolled-back upload is cleaned up. Removing a photo returns the updated profile with a null photo URL. Old/no-photo URLs return 404 when no current photo exists.

Configure `PROFILE_IMAGES_DIR` (or `app.storage.profile-images-dir`) for persistent media storage. Local default: `uploads/profiles`. Public avatar responses use no-store caching so replacements/removals take effect without retaining an old cached photo.

### Change verified email or phone

Step 1 — request verification:

```http
POST /api/v1/users/me/contact-changes
Authorization: Bearer <accessToken>
Content-Type: application/json
```

```json
{
  "channel": "EMAIL",
  "destination": "new-address@example.com",
  "currentPassword": "<current password>"
}
```

For phone, use `"channel": "PHONE"` and an international number such as `+61412345678`.

```json
{
  "challengeId": "<challengeId>",
  "maskedDestination": "ne***@example.com",
  "expiresAt": "<UTC timestamp>",
  "resendAfterSeconds": 60,
  "message": "Verification code sent to your new contact."
}
```

Step 2 — verify and save:

```http
POST /api/v1/users/me/contact-changes/verify
Authorization: Bearer <accessToken>
Content-Type: application/json
```

```json
{ "challengeId": "<challengeId>", "otp": "<six-digit code>" }
```

Returns the updated profile. The existing email/phone remains unchanged until this succeeds; the corresponding verification timestamp is then set. Names, access type, password, and other contact are not changed by this operation. Account identity remains the stable user UUID, so existing sessions and listing ownership remain associated with the same account.

Codes are bound to the authenticated user, channel, and new destination. Only an HMAC hash is stored. Incorrect guesses persist and are limited by the existing `app.otp.max-attempts` setting (currently 5). TTL and resend cooldown use the existing OTP configuration (currently 5 minutes and 60 seconds). Used/expired codes cannot be replayed. Contacts are checked for uniqueness both before delivery and before applying the change.

This uses the existing SMTP and SMS dispatch implementations. Real verification requires the relevant mail/SMS channel to be enabled and configured. Disabled delivery returns 503 rather than pretending to verify a contact. No live test emails or SMS were sent during implementation; tests mock OTP delivery.

## Image fields added to related responses

Existing fields and endpoint routes are retained; these are additive response fields:

- Login and refresh `user`: `profileImageUrl`.
- Listing list/search summaries: `listerProfileImageUrl`.
- Listing detail provider (`lister`): `profileImageUrl`.
- Listing detail voucher entries: `profileImageUrl`.
- Public listing review and Admin review entries: `profileImageUrl`.
- Signup vouch-user search: `profileImageUrl`.
- Recovery Buyer search: `profileImageUrl`.
- Incoming vouch requests: requester's `profileImageUrl`.
- Given-vouch entries: Lister's `profileImageUrl`.
- Review-recipient search: `profileImageUrl`.
- Review invitation details and service-availed history: `buyerProfileImageUrl` and `listerProfileImageUrl`.

The live frontend uses these in provider cards, voucher avatars, service cards, reviews, vouch search/recovery/results/cards, request-review search, service history, dashboard headers/sidebars, and profile pages. Profile mutations refresh the signed-in user's stored identity and affected query caches.

## Frontend testing

1. Restart the backend after applying the schema migration (already applied locally).
2. Open `/buyer-dashboard/profile`, `/lister-dashboard/profile`, or `/admin-dashboard/profile` while signed in to the matching account.
3. Save name, location, and bio; check the success/error feedback.
4. Select a JPEG/PNG photo. Check the profile and dashboard avatar, then visit an owned listing/provider card and vouch/review search results.
5. Upload a replacement and confirm the photo changes. Remove it and confirm the initials fallback.
6. Enter a new email/phone and current password; request a code, retrieve it from the new contact, and verify. Check that the old contact stays until verification succeeds.
7. Try an incorrect code, expired code, duplicate contact, and invalid image to check error feedback.

## Database changes applied locally

In `localhost:3306/druk_connect`:

- Added nullable `users.bio VARCHAR(1000)`.
- Added nullable `users.location VARCHAR(180)`.
- Added nullable `users.profile_image_path VARCHAR(500)`.
- Created `profile_contact_changes`: a pending verification row per user/channel, containing destination, HMAC hash, attempt count, timestamps, and user foreign key.

Existing user names, emails, phone numbers, listing data and uploaded listing images were untouched. Existing accounts start with null photo/bio/location values. The contact-change table was verified empty locally after the test run; test fixtures used an isolated H2 database and temporary image files.

Exact SQL: `src/main/resources/db/manual/2026-10-28-user-profiles.sql` in the backend, also delivered as `user-profile-migration.sql`. Apply once to other databases before starting the updated backend; it was already applied to local MySQL.

Validation: all 53 backend tests passed, including MySQL schema validation and profile/OTP/image propagation tests. Frontend type checking, changed-module lint, and focused avatar/API checks passed.
