# Frontend redesign

## Scope and direction
The original static catalogue is now a bright travel discovery surface with a navy header, prominent trip search, reusable property cards and a responsive filter area. The existing vanilla frontend stack and source photography are retained.

Customers can search, narrow results, compare sample prices, open property details and save favourites. Destination tiles and the resort collection lead to filtered results. Previously nonfunctional account/booking links are not presented as working authentication or checkout.

## Interactions and state
- Search is committed on form submission. Details use the committed trip; changes in the search fields apply after pressing Search.
- Filters and sorting update immediately against the committed trip.
- Check-in cannot be in the past, check-out must be later, and rooms cannot exceed guests.
- Sample capacity and room quantities are checked; nightly inventory is not implemented.
- Search URLs preserve destination, dates, guests and rooms. Filters and saved-only mode are not serialized.
- Saved property IDs are stored under `hotelvista:saved:v1` in localStorage. No passwords, credentials, session tokens or user profiles are stored.
- Unavailable browser storage falls back to memory and informs the user when saving.
- The property dialog uses the native modal dialog element, Escape dismissal and focus restoration.
- The duplicate frontend homepage redirects to the canonical root page while preserving search/hash when JavaScript is available.

## Images
18 existing JPEG assets were resized to at most 1440 × 1000 and encoded as WebP at quality 80. The selected source files total 59,264,745 bytes; their optimized copies total 2,598,566 bytes (95.6% reduction). This measures the selected image files, not page load time. Original files are retained.

## Backend handoff
Keep the UI catalogue adapter boundary while replacing demo data with API responses. The server must:
1. Return properties, room types, amenities and date-specific availability.
2. Calculate final prices, taxes, fees and cancellation terms.
3. Validate quantities and capacity transactionally when reserving.
4. Own authentication, booking ownership and administration.
5. Return loading/error/no-availability states to the frontend.

Do not treat frontend room counts or estimates as an inventory or pricing authority.

## Validation
Dependency-free Node checks cover catalogue matching, aliases, combined filters, sorting, saved stays, capacity limits, calendar validity and multi-room night totals. Static validation checks asset references and JavaScript syntax. A subsequent Chrome browser check verified the desktop layout and a 375 CSS-pixel mobile content viewport (390-pixel iframe including scrollbar). Checked Goa search (two results), ascending price sorting, breakfast filtering (one result), a two-night estimate, saved-state persistence after reload, mobile guest/room controls, a two-room estimate of ₹19,200, and dialog dismissal/focus restoration. The mobile document had no horizontal overflow. This is frontend preview verification, not testing of real availability or a backend.
