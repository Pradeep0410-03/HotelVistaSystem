# Booking confirmation and My bookings

## Try the flow

Start Spring with PostgreSQL using backend/README.md. The hosted static preview has no backend and is not updated by this change. Use the optional inventory demo in docs/room-availability.md, then open http://localhost:8080/index.html.

1. Browse database properties, open details, choose future dates, guests and rooms.
2. Check availability and choose Review booking for a room type.
3. If needed, sign in or register. The room selection is stored in this tab's sessionStorage and restored through a fixed bookings.html return route. No credentials are stored there.
4. Review the refreshed subtotal and uniform cancellation policy. Confirm creates the reservation. Pay at hotel; this project version adds no taxes/fees.
5. My bookings displays the booking reference, dates, quantity, accepted price, policy and status.
6. Cancel before the saved deadline to release inventory. Later requests are rejected with an admin-review message; the admin workflow is not built yet.

The sample catalogue remains a separate demonstration. Only database room options support real confirmation.

## API

All these endpoints require an authenticated session; POST also requires CSRF.

| Endpoint | Behavior |
|---|---|
| POST /api/bookings | Confirm one room type for a stay, or return the previous result for an identical retry |
| GET /api/bookings?page=0 | Current user's bookings, 20 per page, with hasNext |
| POST /api/bookings/{id}/cancel | Cancel own eligible booking; repeat cancellation does not release inventory twice |

Creation takes propertyId, roomTypeId, checkin, checkout, guests, rooms, expectedSubtotal and a UUID requestKey. Identity comes from Spring's authenticated principal. The client cannot choose the booking owner, status, final price or policy. expectedSubtotal is only a comparison against the server calculation; a changed price returns 409 for review.

## Transaction and concurrency

BookingService uses one Spring transaction with parameterized JdbcTemplate SQL. It locks the account row to serialize same-account requests, then the active property, room type and nightly inventory rows. Nights are locked in date order. It rechecks room capacity, all nights, available quantity and current price before inserting booking/item records and incrementing reserved quantities.

V2 adds a request_key UUID and unique (user_id, request_key) index. A repeated key for the same payload returns its existing booking; a changed payload gets 409. The UI preserves the key across uncertain POST outcomes and does not automatically retry writes. A new intentional selection gets a new key, so users should inspect My bookings after an uncertain result.

Cancellation uses the same account/room locking order and releases every night in one transaction. A cancelled booking remains in history. The saved cancellation deadline and accepted policy are used, rather than recalculating from later property changes.

A separate user competing for the last room waits on the room lock, then sees the updated inventory and receives 409. Any transactional failure rolls back its writes. Future admin inventory writes must follow compatible locking rules; no such endpoints exist yet.

## Scope

One room type per booking; no mixed room bundles, online payment, email confirmation, admin review interface, date changes or automated completion of past stays. Names in My bookings reflect the current property/room names; rates and policy are stored snapshots. There is no external backend deployment in this PR.

## Verification

Run node --test tests/*.test.cjs. Booking-client checks cover CSRF, excluded owner/status fields, surfaced conflicts and no automatic retry.

Run mvn -Pintegration verify in backend with a disposable PostgreSQL database. BookingIT checks HTTP creation/cancellation, owner isolation, CSRF/anonymous protection, same-key retry, changed payload/price rejection, cancellation deadline, and a real concurrent race for the last room. Fixtures are cleaned up after each test. FrontendIT verifies the booking page is packaged and publicly readable. Browser visual QA is not claimed.
