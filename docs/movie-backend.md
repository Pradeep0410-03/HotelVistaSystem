# Movie reservation backend

The movie module shares Spring sessions, password policy, CSRF protection and user accounts with hotel booking. It has separate PostgreSQL inventory because seats belong to a single screening, not a range of hotel nights. Existing hotel tables and APIs are unchanged.

## API

| Method and path | Access | Purpose |
|---|---|---|
| GET /api/movies?city=Bhopal&page=0 | Public | Movies with upcoming shows; 20 per page |
| GET /api/movies/{id}/shows?city=Bhopal&page=0 | Public | Upcoming screenings with cinema and local timezone |
| GET /api/movie-shows/{id}/seats | Public | Seat labels, INR prices and availability, without owner data |
| POST /api/movie-bookings | Signed in + CSRF | Reserve 1–10 distinct seats |
| GET /api/movie-bookings?page=0 | Signed in | Own reservations and seat price snapshots |
| POST /api/movie-bookings/{id}/cancel | Owner + CSRF | Cancel before showtime and release seats |
| POST /api/admin/movies | Admin + CSRF | Create movie: title, language, durationMinutes |
| POST /api/admin/movies/cinemas | Admin + CSRF | Create cinema: name, city, address, timezone |
| POST /api/admin/movies/shows | Admin + CSRF | Create screening and seat inventory |

Reservation JSON:

```json
{"showId":1,"seats":["A1","A2"],"expectedTotal":500.00,"requestKey":"a85a384c-a7d2-4aca-af0c-d3184b18f789"}
```

Use actual show IDs from the catalogue. Generate a new UUID for each new selection, and reuse it only when retrying that same submission. The expected total is a price-change check; the server sums current database seat prices. Responses use database-style snake_case fields, e.g. total_amount, show_id and starts_at. Page wrappers use items, page and hasNext.

Screening creation JSON:

```json
{"movieId":1,"cinemaId":1,"startsAt":"2027-01-20T18:00:00+05:30","seats":[{"label":"A1","price":250},{"label":"A2","price":250}]}
```

## Concurrency and history

Mutations lock the user row, then the show row, then chosen seats. All seat validation, server pricing, booking creation and seat assignment commit in one transaction. A competing customer gets 409; a multi-seat request cannot reserve a partial selection. Seat rows contain at most one current reservation. Composite foreign keys prevent claims referring to another screening.

An owner/request-key unique constraint and a canonical selection fingerprint protect retries. Reusing a key for different seats, show or expected price returns 409. Retrying a cancelled reservation returns that cancelled record rather than making a new purchase. Cancellation clears the current claims but retains historical seats, agreed prices and accepted policy. Access to another customer's reservation returns 404.

## Demo mode and limits

The existing `local,demo` startup profiles also seed 3 fictional movies in Bhopal, Delhi and Mumbai, 9 upcoming screenings and 40 seats per screening. A repeated start on the same date preserves existing claims. Later dates add a new upcoming demo window; old reservations are retained. All data is illustrative.

No money is collected. This phase confirms demonstration seat reservations immediately, permits free cancellation until screening start, and does not implement payment holds, refunds or real ticket delivery. Before taking payments, add expiring holds, gateway confirmation and webhook idempotency. Physical auditorium scheduling, seat categories, certificate/age rules, catalogue editing and admin audit for movie changes remain later work.

The Movies page now calls these APIs for city search, showtimes, seat selection, review, confirmation and cancellation. The shared account page accepts only the fixed return=movies destination. Selection and retry keys remain in sessionStorage in the current browser tab; passwords and session tokens are never stored there. Unknown write outcomes retain the same request key and freeze edits until retried or explicitly discarded. A fresh availability/price check precedes initial confirmation; the database remains authoritative. Flyway V5 applies additively to the existing database. Run `mvn -Pintegration verify` against a disposable PostgreSQL database for ownership, CSRF, stale price, cancellation, duplicate request and seat-race checks.
