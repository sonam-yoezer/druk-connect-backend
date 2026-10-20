# Dashboard summaries: Lister, Buyer and Admin

All three endpoints are GET requests with no body. Use `Authorization: Bearer <accessToken>` from normal login, not a vouch-recovery token. Responses have `Cache-Control: no-store`. Each response's `generatedAt` is a UTC timestamp. The following numbers are illustrative, not live database values.

## Lister dashboard

**Method:** GET  
**Path:** `http://localhost:8080/api/v1/lister/dashboard`  
**Authentication:** Bearer token for an active LISTER account.

```json
{
  "totalListings": 3,
  "activeListings": 2,
  "inactiveListings": 1,
  "totalListingViews": 126,
  "averageRating": 4.8,
  "approvedReviewCount": 12,
  "activeVouchCount": 2,
  "servicesAvailedCount": 8,
  "uniqueBuyersServed": 6,
  "servicesAvailedLast30Days": 3,
  "generatedAt": "2026-10-19T06:00:00Z"
}
```

The existing Lister endpoint is retained. Listing, review and service metrics cover only that Lister's non-deleted listings, including inactive listings. Views are lifetime totals, not monthly totals or unique visitors. Rating averages individual approved reviews, rounded to one decimal; it is `null` when no approved reviews exist. Vouches are current active vouches received.

## Buyer dashboard

**Method:** GET  
**Path:** `http://localhost:8080/api/v1/buyer/dashboard`  
**Authentication:** Bearer token for an active BUYER account.

```json
{
  "servicesAvailedCount": 8,
  "uniqueProvidersUsed": 5,
  "servicesAvailedLast30Days": 2,
  "submittedReviewCount": 4,
  "approvedReviewCount": 3,
  "pendingReviewCount": 1,
  "rejectedReviewCount": 0,
  "pendingReviewInvitationCount": 2,
  "pendingVouchRequestsReceived": 1,
  "activeVouchesGiven": 4,
  "generatedAt": "2026-10-19T06:00:00Z"
}
```

| Field | Meaning |
|---|---|
| `servicesAvailedCount` | The Buyer's service-availed records for non-deleted listings, through generatedAt. These are provider-marked records, not payment/booking confirmations. |
| `uniqueProvidersUsed` | Distinct Listers across those service records. |
| `servicesAvailedLast30Days` | Records in the rolling 30 days ending at generatedAt. |
| `submittedReviewCount` | All the Buyer's review submissions for non-deleted listings. Includes approved, pending and rejected submissions; resubmissions count separately. |
| `approvedReviewCount` | The Buyer's approved submissions within that scope. |
| `pendingReviewCount` | The Buyer's submissions awaiting Admin moderation within that scope. |
| `rejectedReviewCount` | The Buyer's rejected submissions within that scope. |
| `pendingReviewInvitationCount` | Emailed, unexpired PENDING invitations for active listings where the Buyer does not already have a pending/approved review. Inactive/deleted listing invitations are excluded. |
| `pendingVouchRequestsReceived` | Vouch requests addressed to this Buyer with PENDING status. These are distinct from review invitations or service requests. |
| `activeVouchesGiven` | Current ACTIVE vouches given by this Buyer; revoked vouches are excluded. |

No caller-supplied user ID is required or honored. Another Buyer's records are never included.

## Admin dashboard

**Method:** GET  
**Path:** `http://localhost:8080/api/v1/admin/dashboard`  
**Authentication:** Bearer token with ADMIN authority and a current active ADMIN role in the database. The account itself must also be active. An Admin may have BUYER or LISTER access type; role membership determines access to this endpoint.

```json
{
  "totalUsers": 2486,
  "activeUsers": 2300,
  "newUsersLast7Days": 24,
  "totalListings": 170,
  "activeListings": 158,
  "newListingsLast7Days": 8,
  "pendingReviewCount": 7,
  "pendingVouchWithdrawalCount": 12,
  "pendingScamReportCount": 3,
  "pendingAppealCount": 1,
  "unresolvedFeedbackCount": 5,
  "generatedAt": "2026-10-19T06:00:00Z"
}
```

| Field | Meaning |
|---|---|
| `totalUsers` | Platform users excluding DELETED accounts. |
| `activeUsers` | Accounts with ACTIVE status, regardless of Buyer/Lister/Admin role. |
| `newUsersLast7Days` | Non-deleted users created in the rolling 7 days ending at generatedAt. |
| `totalListings` | All platform listings except DELETED. |
| `activeListings` | All ACTIVE platform listings. |
| `newListingsLast7Days` | Non-deleted listings created in that rolling 7-day window. |
| `pendingReviewCount` | All PENDING reviews in the Admin review-moderation queue. |
| `pendingVouchWithdrawalCount` | PENDING withdrawal requests requiring Admin action. Ordinary Buyer-to-Lister vouch requests are not Admin moderation tasks. |
| `pendingScamReportCount` | PENDING scam reports. |
| `pendingAppealCount` | PENDING scam-report appeals. |
| `unresolvedFeedbackCount` | Feedback with NEW or IN_PROGRESS status; RESOLVED feedback is excluded. |

Admin moderation counts match their status-based queues, including unresolved items relating to archived listings. Do not add them together and label the result "unique users needing attention": a single account can have multiple queue items.

## Scope and handling

Use separate endpoints for the three views rather than one response that mixes private Buyer data with global Admin statistics. Existing login provides roles and accessType for choosing the appropriate dashboard. The backend independently checks authorization.

The screenshots contain placeholder message, saved-service, enquiry and profile-completeness metrics. These are not supplied because the backend does not currently track the corresponding data. No invented values are returned. These endpoints provide summary numbers, not activity-feed or recommendation lists; existing listing/history/moderation APIs remain available for detailed sections.

Time windows are rolling UTC intervals, not calendar weeks or months. Empty counts return 0. Invalid/missing normal tokens receive 401; wrong account types, inactive accounts or missing current Admin role receive 403. An unknown JWT user receives 404.

## Changes and testing

- Backend only. The frontend and database schema/data are unchanged.
- Existing Lister endpoint is preserved.
- Buyer and Admin statistics use aggregate queries over existing entities; no whole-history loads or new tables.
- Tests cover empty dashboards, Buyer data isolation, service/review status counts, invitation expiry/delivery, active/revoked vouches, platform counts, rolling windows, authentication and stale/missing Admin roles.
- Restart the backend before testing these endpoints.
