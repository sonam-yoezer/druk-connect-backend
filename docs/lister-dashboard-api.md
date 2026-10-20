# Lister Dashboard Summary

**Method:** GET

**Path:** `http://localhost:8080/api/v1/lister/dashboard`

**Authentication:** `Authorization: Bearer <accessToken>`

**Account:** An active LISTER account. Use the normal login access token; a vouch recovery token is not accepted. No user ID parameter is required or used: the endpoint always summarizes the authenticated account.

**Request body:** None.

**Example response (illustrative values):**

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

## Metric meanings

| Field | Meaning |
|---|---|
| `totalListings` | All owned listings except DELETED. |
| `activeListings` | Owned ACTIVE listings. |
| `inactiveListings` | Owned INACTIVE listings. |
| `totalListingViews` | Lifetime sum of the stored view counters across non-deleted owned listings. This is not a monthly view count or a count of unique visitors. |
| `averageRating` | Average of individual APPROVED review ratings across non-deleted owned listings, rounded to one decimal. Returns `null` if there are no approved reviews; display “Not rated yet.” |
| `approvedReviewCount` | Number of APPROVED reviews across non-deleted owned listings. Pending/rejected reviews are excluded. |
| `activeVouchCount` | Current ACTIVE vouches received by this Lister. |
| `servicesAvailedCount` | Number of service-availed records across non-deleted owned listings through `generatedAt`. These are provider-marked records, not WhatsApp clicks or payment-confirmed bookings. |
| `uniqueBuyersServed` | Distinct Buyers across those service-availed records. One Buyer who availed two listings counts once here and twice in servicesAvailedCount. |
| `servicesAvailedLast30Days` | Service-availed records between generatedAt minus 30 days and generatedAt, inclusive, using UTC instants. This is a rolling 30-day window, not a calendar month. |
| `generatedAt` | UTC timestamp used to generate this summary and bound its rolling period. |

Listing/review/service metrics include inactive listings but exclude deleted ones. Views come from the existing counters; no dated view-event records exist, so no “views this month” metric is invented. Likewise, the endpoint reports the real service-availed history rather than placeholder enquiry/request totals or guessed profile completeness.

## Request example

```bash
curl --request GET \
  'http://localhost:8080/api/v1/lister/dashboard' \
  --header 'Authorization: Bearer <accessToken>'
```

200 responses include `Cache-Control: no-store` to avoid caching private account statistics.

## Error responses

- `401`: No valid normal access token.
- `403`, code `ACTIVE_LISTER_REQUIRED`: The authenticated account is a Buyer or is not active.
- `404`, code `USER_NOT_FOUND`: The JWT subject no longer corresponds to a user.

## Implementation and verification

- Backend-only change; frontend untouched.
- Reads existing `listings`, `listing_reviews`, `listing_service_availments`, `vouches`, and `users` records through aggregate repository queries.
- No database schema or data changes, migration, or new tables.
- Tests cover empty results, account isolation, deleted/inactive listings, weighted review aggregation, decimal rounding, distinct Buyers, rolling dates, future timestamps, authentication, and account type/status checks.

Restart the backend before testing the new endpoint.
