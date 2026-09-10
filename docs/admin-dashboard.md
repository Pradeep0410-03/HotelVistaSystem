# Administration

Open `/admin.html` after signing in with an ADMIN account. The account navigation shows an Administration link for admins. The HTML shell is public; every data endpoint under `/api/admin/**` requires ADMIN and writes require the session's CSRF token. Registration always creates customers.

## First administrator

There is no default administrator password or public role-upgrade endpoint. A database operator must promote one existing, verified account once:

```sql
UPDATE users SET role='ADMIN' WHERE lower(email)=lower('your-existing-account@example.com');
```

Replace the example with the intended registered account and verify exactly one row changed. Sign out and sign in again to refresh authorities. Keep database credentials private. Role changes do not revoke existing sessions automatically; revoke sessions/restart the single instance when removing privileges.

## Daily work

- Search properties by city; add or edit a property. New properties use Asia/Kolkata, matching this Indian catalogue.
- Manage room types, capacity, total rooms, price and active status.
- Set nightly sellable quantities over an inclusive start/exclusive end range. Missing nights are unavailable. Existing reservations are preserved. Quantities cannot exceed room totals or fall below reserved quantities.
- Deactivate listings instead of deleting booking history.
- Review paginated bookings. Admin cancellation requires a reason, can override the normal free-cancellation deadline, and records actor/time/reason in admin_cancellations. Repeated cancellation releases inventory once.

Guest requests are currently received outside the application; this is an administrator review action, not an in-app support/request inbox. Payments/refunds remain out of scope (pay at hotel).

Booking locks are reused for cancellations. Inventory updates hold the room lock before updating nights, matching reservation serialization. Prices saved on existing bookings remain unchanged when catalogue prices change. Form edits currently use last-write-wins; coordinate administrators when editing the same listing. A full audit trail for catalogue edits and automatic session revocation are future production hardening work.

## Verification

AdminIT covers anonymous/customer denial, admin access, CSRF, protected inventory and audited cancellation replay. Existing booking concurrency tests continue to cover last-room races. Browser visual verification remains pending; no deployment is included.
