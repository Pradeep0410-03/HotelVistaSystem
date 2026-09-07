# Hotel Vista

A responsive accommodation discovery frontend for a multi-property booking platform, written in HTML, CSS and JavaScript.

## Run

Open the root `index.html`, or use the dependency-free Node preview (Node 20+):

```sh
npm run dev
```

Visit http://localhost:5173. The preview serves public frontend assets only, without exposing backend or Git files. No package installation is required.

Alternatively, serve the repository root with Python:

```sh
python3 -m http.server 8000
```

Visit http://localhost:8000. There is no frontend dependency installation or build step. The empty `backend/package.json` is a legacy placeholder, not a runnable Node application.

## What works

- Destination/property-name search with Delhi/New Delhi and Bangalore/Bengaluru aliases.
- Check-in/check-out validation and guest/room controls.
- Budget, property-type and amenity filters, sorting and empty results.
- Device-local saved stays (localStorage, with an in-memory fallback).
- Accessible native property-detail dialog and an estimated trip subtotal.
- Search parameters in the URL for reopening the same trip.
- Responsive layouts, reduced-motion support, keyboard focus and optimized WebP assets.

## Demo boundaries

All property names, descriptions, capacities, quantities and prices in `js/properties.js` are sample data. Photos are illustrative assets retained from the original repository. No rating or review counts are presented as real.

Search filters sample capacity and quantities; it does **not** check date-specific inventory. A property detail shows an estimate, not a guaranteed price. No booking, payment, account or authentication is created. Taxes and fees are excluded from estimates. Saved stays are local to the browser/device, not an account.

## Structure

- `index.html`: canonical frontend entry.
- `css/style.css`: shared theme, layout, responsive and interaction states.
- `js/properties.js`: sample catalogue adapter.
- `js/catalogue.js`: pure matching, date validation and estimate logic.
- `js/app.js`: UI state and events.
- `assets/optimized/`: WebP copies used by the current UI.
- `frontend/index.html`: redirect to the canonical entry.
- `docs/frontend-redesign.md`: decisions and backend integration boundaries.
- `tests/catalogue.test.cjs`: dependency-free logic checks.
- `backend/`: existing, unimplemented placeholders.

Older standalone scripts and original JPGs remain for reference; the redesigned page does not load them.

## Check

```sh
node --test tests/catalogue.test.cjs
node --check js/app.js
```

The next backend phase should replace the sample catalogue with Spring API responses and implement server-owned availability, pricing, authentication and booking authorization.
