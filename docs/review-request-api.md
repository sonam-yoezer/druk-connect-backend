# Review request API

Backend implementation: 11 October 2026. Frontend integration is intentionally paused.

All endpoints below use `Authorization: Bearer <accessToken>` (normal login access token, not a vouch recovery token). User identities come from the JWT. No endpoint signs a recipient in automatically.

## Rules

- Only active Buyers can submit a review of an active listing.
- A Buyer qualifies after recording a WhatsApp click for that listing OR receiving an emailed review invitation from the listing owner.
- Only an active Lister who owns the listing can search recipients, send invitations, or list requests.
- One pending or approved review per Buyer/listing; rejected reviews may be resubmitted while eligible.
- Only the intended Buyer can resolve an invitation. Invitations expire after 30 days.
- A duplicate unexpired pending invitation returns 409 and does not send another email. Expired invitations can be renewed.
- Email failure returns 503 `REVIEW_REQUEST_EMAIL_FAILED` and rolls back the invitation; it does not grant review access.
- Requests become `COMPLETED` on successful review submission. Reviews still await the existing admin moderation flow.

## 1. Record WhatsApp contact

```http
POST /api/v1/listings/{listingId}/review-contact
Authorization: Bearer <buyerAccessToken>
```

No body. Idempotent: repeated clicks create only one contact record. Call this before opening WhatsApp when frontend integration resumes. It records a click intent, not proof of an actual conversation, booking, or WhatsApp return.

Response: the eligibility object below. Future frontend should display the review prompt on returning/focusing the app; the backend cannot observe navigation inside WhatsApp.

## 2. Check whether the current Buyer can review

```http
GET /api/v1/listings/{listingId}/review-eligibility
Authorization: Bearer <buyerAccessToken>
```

```json
{
  "listingId": "<listingId>",
  "canReview": true,
  "whatsappContacted": false,
  "invited": true,
  "reviewAlreadyExists": false,
  "message": "You can review this service."
}
```

## 3. Search registered review recipients

```http
GET /api/v1/listings/{listingId}/review-requests/buyers/search?query=sonam&page=1&size=10
Authorization: Bearer <listerAccessToken>
```

Search active Buyers by first name, last name, full name, email, or phone number. Query length: 2–100 characters; page starts at 1; size is capped at 50.

```json
{
  "buyers": [{
    "userId": "<buyerId>",
    "firstName": "Sonam",
    "lastName": "Dorji",
    "email": "buyer@example.com",
    "phoneNumber": "+61412345678",
    "whatsappContacted": false,
    "requestPending": false,
    "alreadyReviewed": false
  }],
  "currentPage": 1,
  "pageSize": 10,
  "totalElements": 1,
  "totalPages": 1,
  "hasNext": false,
  "hasPrevious": false
}
```

Disable request buttons for `requestPending` or `alreadyReviewed` in the future frontend.

## 4. Send a review request email

```http
POST /api/v1/listings/{listingId}/review-requests
Authorization: Bearer <listerAccessToken>
Content-Type: application/json
```

```json
{ "buyerUserId": "<buyerId>" }
```

201 response:

```json
{
  "requestId": "<requestId>",
  "listingId": "<listingId>",
  "listingTitle": "Home Tour",
  "buyerUserId": "<buyerId>",
  "buyerName": "Sonam Dorji",
  "status": "PENDING",
  "requestedAt": "<UTC timestamp>",
  "expiresAt": "<UTC timestamp, 30 days later>",
  "reviewUrl": "http://localhost:3000/buyer-dashboard/services/<listingId>?reviewRequest=<requestId>&review=true",
  "loginUrl": "http://localhost:3000/auth/login?redirect=<URL-encoded review path>",
  "message": "Review request email sent successfully."
}
```

The email names the requesting Lister and the service, links to `reviewUrl`, and tells the Buyer to sign in using the account associated with the recipient email. The link is not an authentication token. Successful delivery means the configured mail sender accepted the send operation, not a guarantee that the message reaches the inbox.

## 5. List requests for an owned listing

```http
GET /api/v1/listings/{listingId}/review-requests?page=1&size=10
Authorization: Bearer <listerAccessToken>
```

Response: `requests` array of invitation response objects, plus `currentPage`, `pageSize`, `totalElements`, `totalPages`, `hasNext`, and `hasPrevious`. Expired pending requests are returned with status `EXPIRED`.

## 6. Resolve the email invitation after login

```http
GET /api/v1/review-requests/{requestId}
Authorization: Bearer <buyerAccessToken>
```

Returns the invitation response above. Another Buyer receives 403; an expired invitation receives 410. Only active listings are available.

## 7. Submit a review using the existing endpoint

```http
POST /api/v1/listings/{listingId}/reviews
Authorization: Bearer <buyerAccessToken>
Content-Type: application/json
```

```json
{ "rating": 5, "comment": "Great service.", "tags": ["Friendly"] }
```

Existing response/moderation behavior is retained. The endpoint now enforces eligibility server-side. Rating must be 1–5, comment is required and limited to 2000 characters, and the existing tag limits apply. Approved public review reads are unchanged.

## Frontend work intentionally left pending

1. Add Request Review to owned listing details and integrate recipient search/request/list endpoints.
2. Record WhatsApp contact before opening the external app; use eligibility when showing a review prompt on return.
3. Preserve the complete invitation path through login. The current frontend redirects successful logins to the dashboard, so email links do not yet reliably return a logged-out Buyer to the review modal.
4. On the Buyer detail route, read `reviewRequest` and `review=true`, resolve the invitation using the authenticated recipient account, check eligibility, and open the existing review modal.
5. Handle wrong accounts, expired requests, duplicate reviews, and mail errors with the API responses above.

Until this integration is added, the existing frontend does not record WhatsApp contacts. Its review POST will be rejected unless the Buyer already has a valid backend invitation or contact record.

## Database changes actually applied

Database: local MySQL at `localhost:3306`, schema `druk_connect`.

Created two InnoDB tables with CHAR(36) UUID keys matching the existing database collation:

- `listing_review_contacts`: listing, Buyer, and first WhatsApp click timestamp. Unique `(listing_id, buyer_user_id)`; foreign keys to `listings` and `users`.
- `listing_review_requests`: listing, Buyer, status, request/expiry timestamps, email send timestamp, and completion timestamp. Unique `(listing_id, buyer_user_id)`; foreign keys and lookup indexes. Stored statuses are `PENDING` and `COMPLETED`; `EXPIRED` is computed for the API.

No existing tables or existing user/listing/review records were altered or deleted. No historical contact records were invented. Both new tables were verified empty after creation; tests used a separate in-memory H2 database.

Migration: `src/main/resources/db/manual/2026-10-11-review-requests.sql`. The project uses Hibernate schema validation and has no migration runner, so apply this additive SQL to each deployment before restarting the backend. It was already applied to the local database.

Set `FRONTEND_URL` to the deployed frontend origin. This feeds `app.review.frontend-url`; the local default is `http://localhost:3000`. Configure existing `app.mail.from`, `app.mail.enabled`, and SMTP settings for real delivery.

## Validation

- Backend compilation passed.
- 12 unit tests and 6 H2 integration tests passed, covering eligibility, duplicates, recipient identity, phone/email/name search, invitation completion, mail failure rollback, and authenticated routing.
- Emails were mocked in tests; no real user received a verification email.
- The existing application context test also passed against local MySQL with `ddl-auto=validate`, confirming the new mappings match the applied schema.
