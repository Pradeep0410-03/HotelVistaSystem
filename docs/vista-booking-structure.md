# Vista Booking: category foundation

## Scope

The homepage offers three photographic choices and shared account access. Hotels retain the working reservation system. Movies use a cinema-inspired burgundy theme; Sports use a green stadium theme. Both clearly state that reservations are coming next. They do not fabricate successful ticket purchases.

## Shared versus category-specific logic

Accounts, navigation and API error handling can be shared. Availability cannot be identical: hotels reserve quantities across nights; movie shows reserve seats at a screening; sports events reserve seats or capacity within ticket sections. Each needs its own inventory model and transaction constraints. Payment integration is a later phase.

## Folder conventions

Add page markup in `frontend/pages`, controllers in `frontend/js/pages`, API clients in `frontend/js/api`, and reusable helpers in `frontend/js/shared`. Keep static images under `assets`. Historical booking-intent helpers remain in `frontend/js/legacy` until old saved selections can be safely retired.

Spring packages the canonical frontend files as static resources. Root login/register/bookings/admin routes redirect while preserving the query and hash, so prior authentication links continue working. API security and existing PostgreSQL migrations remain unchanged. Do not rename database or storage identifiers just to change the displayed brand.

## Styling

Tailwind 4.3.3 is pinned with a lockfile. Run `npm ci` then `npm run build:css`. The generated CSS is committed and packaged by Maven. Tailwind preflight is omitted to avoid resetting established hotel forms; new components are scoped to the new category surface. Responsive layouts collapse on small screens and respect reduced-motion preferences.

## Images

Category photographs are illustrative, not representations of bookable movie or sports inventory. Cinema: Myke Simon, Unsplash https://unsplash.com/photos/atsUqIm3wxo . Stadium image source: https://images.unsplash.com/photo-1522778119026-d647f0596c20 . Hotel imagery reuses the existing optimized image collection.

## Next backend milestone

Define shows/events and venues, implement inventory and booking transactions, then connect the category-specific forms to those APIs. Do not expose a reservation button until server-side availability, ownership and duplicate-submission protection are implemented.
