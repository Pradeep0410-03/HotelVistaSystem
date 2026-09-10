# Vista Booking

A booking project built with Spring Boot, PostgreSQL and HTML/JavaScript. The Tailwind-styled homepage leads to Hotels, Movies and Sports, each with its own interface.

## Run the integrated application

Follow [backend setup](backend/README.md) to configure Java 17, Maven and PostgreSQL. From `backend/`:

```sh
mvn spring-boot:run -Dspring-boot.run.profiles=local,demo
```

Open http://localhost:8080/. The opt-in demo profile seeds 750 fictional hotel properties and inventory while preserving existing reservations. Use `local` without `demo` to skip seeding. No frontend build is needed to run the committed application.

Hotels support accounts, city search, availability, booking confirmation, My bookings and eligible cancellation. The admin API manages properties, room inventory and audited cancellations. Movies and Sports currently have separate introductory pages; tickets, seats and payments are not implemented.

## Frontend development

Node 20+ is required only for frontend tooling:

```sh
npm ci
npm run build:css
npm test
npm run dev
```

The static preview at http://localhost:5173/ does not provide authentication or database APIs. Test the complete flow through Spring on port 8080. After changing Tailwind classes, rebuild and commit `frontend/styles/vista.css`; production does not use a Tailwind CDN.

## Structure

| Path | Purpose |
|---|---|
| `index.html` | Three-category homepage |
| `frontend/pages/` | Hotels, movies, sports, accounts, bookings and admin |
| `frontend/js/api/` | Same-origin API clients |
| `frontend/js/pages/` | Page controllers |
| `frontend/js/shared/` | Session navigation and stay presentation |
| `frontend/js/legacy/` | Compatibility helpers for earlier saved booking intents |
| `frontend/styles/` | Tailwind source/output and existing hotel/account styles |
| `assets/` | Local optimized hotel and category images |
| `backend/` | Spring application, migrations, Maven and Java tests |
| `tests/`, `scripts/` | Frontend tests and static preview server |
| `docs/` | Architecture, workflows and project notes |

Root account, bookings and admin HTML files are compatibility redirects. The database name, Java package and browser storage keys retain their existing identifiers so the visual rename does not invalidate existing data.

See [category architecture](docs/vista-booking-structure.md), [authentication](docs/authentication.md), [availability](docs/room-availability.md), [bookings](docs/booking-workflow.md) and [deployment readiness](docs/project-readiness.md).

GitHub Actions builds the frontend styles and runs JavaScript tests plus `mvn -Pintegration verify` against a disposable PostgreSQL database.
