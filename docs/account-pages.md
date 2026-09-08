# Phase: account pages and selected-stay handoff

## What changed and why
Customers now have Sign in and Register entry points. A selected property's Reserve link opens Sign in with that trip visible. Switching between account pages and returning to the catalogue preserves the property, dates, guests and room quantity.

This is frontend work only. Valid form submission shows an honest unavailable-service message. It does not create accounts, sessions or bookings. No passwords or email addresses are sent, logged or persisted. No arbitrary return URL is accepted.

## Read the code in this order
1. `login.html` and `register.html`: labelled fields and native validation.
2. `css/account.css`: responsive layout reusing the homepage palette.
3. `js/booking-intent.js`: allowlisted page links and validation of selection parameters.
4. `js/account.js`: trip summary, password visibility, confirmation validation and preview-only submission.
5. `js/app.js`: Reserve handoff and reopening the selected property on return.

Example: view Palm & Tide → Reserve → Sign in → Create an account → Change your selection. The same dates and quantity travel through those pages as query parameters. Only selection identifiers/counts/dates are carried, never a trusted price. Reloading still reconstructs the selection without browser storage. This is not a room hold.

The current catalogue has one sample accommodation option per property. Named room types and per-night inventory remain for the database/catalogue phase; do not mistake the sample quantity checks for live availability.

## Next backend phase
Replace preview submission with same-origin Spring authentication protected by CSRF. Hash passwords on the server, establish an HttpOnly session, then return to booking review after success. Recheck prices, capacity and every night's inventory on the server before explicit booking confirmation. Never trust query parameters for authorization or final prices.

## Checks
Run `npm test` for catalogue and trip handoff validation. Syntax and local asset checks also cover the account pages. Browser visual testing has not been performed for this phase.
