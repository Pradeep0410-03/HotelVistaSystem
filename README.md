# Hotel Vista

A multi-property accommodation project built in phases using HTML, CSS, JavaScript, Spring Boot and PostgreSQL.

## Run the integrated application

Follow [backend setup](backend/README.md) to configure Java, Maven and PostgreSQL. From `backend/`:

```sh
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Open http://localhost:8080/index.html. Use the local profile only for local HTTP development.

For a static frontend preview only, run `npm run dev` from the repository root (Node 20+) and open http://localhost:5173. This preview does not run Java or provide account/database APIs.

## Current capabilities

- Registration, session login and logout through Spring Security.
- Database property browsing, city search, pagination and details.
- Date-based room availability and server-calculated INR subtotals.
- Uniform pay-at-hotel cancellation policy with a property-local deadline.
- A separate sample booking flow with filters, saved stays and illustrative prices.

Booking confirmation, inventory reservation, My bookings and eligible cancellation are implemented. Administration remains a later phase. Availability checks alone do not hold rooms. The hosted static preview is separate from the integrated application.

## Project structure

| Path | Purpose |
|---|---|
| `index.html`, `login.html`, `register.html`, `bookings.html` | Frontend pages |
| `css/` | Styles |
| `js/` | Frontend UI, API clients and sample catalogue |
| `assets/optimized/` | Images used by the frontend |
| `backend/pom.xml` | Java dependencies and Maven build |
| `backend/src/main/java/com/hotelvista/` | Spring application: auth, property, room, config and common packages |
| `backend/src/main/resources/` | Application configuration and Flyway migration |
| `backend/src/test/` | Java tests |
| `backend/tests/` | Standalone SQL schema checks |
| `tests/` | Frontend logic and API-client tests |
| `scripts/` | Static preview server |
| `docs/` | Phase explanations and design notes |
| `.github/workflows/` | Automated checks |

The original empty Node backend folders have been removed. The working security configuration is in `backend/src/main/java/com/hotelvista/config/`. Root `package.json` runs frontend tooling; `backend/pom.xml` builds the backend.

`frontend/index.html` remains a compatibility redirect. Unused standalone frontend scripts and original JPG assets have been removed; optimized images remain.

## Learn and verify

Read [authentication](docs/authentication.md), [frontend integration](docs/frontend-auth-integration.md), [property browsing](docs/property-browsing.md) [room availability](docs/room-availability.md) and [booking workflow](docs/booking-workflow.md).

```sh
node --test tests/*.test.cjs
```

From `backend/`, run `mvn test` or `mvn -Pintegration verify` with a disposable PostgreSQL database configured. GitHub Actions runs frontend and PostgreSQL-backed checks.

## Demo catalogue and refreshed interface

See [project readiness](docs/project-readiness.md) for the architecture review and remaining deployment requirements. After configuring PostgreSQL, start from `backend/` with:

```sh
mvn spring-boot:run -Dspring-boot.run.profiles=local,demo
```

This opt-in profile adds 750 fictional properties across 30 cities, room types and 90 days of inventory. Rerunning preserves existing reservations. Open http://localhost:8080/index.html and use Browse properties. The old sample UI is a separate non-bookable preview. Generated listings are not real businesses.
