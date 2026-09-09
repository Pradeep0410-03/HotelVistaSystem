# Project structure and pre-deployment review

## What changed

The original sample cards and database records looked like equally valid booking paths. Database browsing is now the default even for old destination links. The legacy UI preview requires an explicit choice and remains labelled non-bookable. A shared product stylesheet covers catalogue cards, city shortcuts, account pages and booking review; no framework/runtime migration is required.

The frontend remains plain HTML/CSS/JavaScript. A separate css/project-ui.css keeps presentation changes away from API logic. Backend feature packages remain auth, property, room and booking; demo fixture loading has its own demo package and db/demo resource directory. Flyway alone manages schema versions.

Unused original scripts and JPG assets were removed from the current tree after checking references; Git history retains them. The optimized images remain. A zero-byte Bengaluru photo reference was replaced with a valid existing WebP. Historic phase documents describe earlier milestones, while this guide and the root README describe the current setup.

## Populate a project database

After updating the extracted project files, stop Spring with Ctrl+C. Keep your existing DB_URL, DB_USERNAME and DB_PASSWORD configuration. From backend, start:

```sh
mvn spring-boot:run -Dspring-boot.run.profiles=local,demo
```

V3 adds a nullable unique demo key. The explicit demo profile then creates 750 fictional properties across 30 Indian cities, 1,950 room types and 175,500 inventory rows covering today plus the next 89 days. Villas have one Entire villa unit; other properties have Standard, Deluxe and Family suite room types with varied capacities, quantities and prices.

Data generation is deterministic rather than random on each run. Existing properties, prices and inventory rows are not overwritten. Rerunning fills missing future inventory days without resetting reservations. No accounts, ratings, reviews or payments are fabricated. Generated names and addresses are visibly labelled Demo; photographs are illustrative.

You can omit demo on later starts; the saved records remain. Removing the profile does not delete data. No migration drops or clears existing records. Two simultaneous loaders serialize using a PostgreSQL advisory transaction lock. Do not run the demo profile against a real customer catalogue.

City search accepts full city names, case-insensitively. New Delhi, Bangalore, Pondicherry and Mysore map to their canonical city names. The UI suggests supported demo cities. This is property discovery; only the per-property date check confirms nightly availability.

## Reliability review

Already implemented: transactional reservation/cancellation, inventory locks, server-owned price calculation, unique per-account request keys, owner checks, CSRF protection, hashed passwords, bounded list pagination, stale-search response protection and safe text rendering. The test suite includes competing customers for the last room.

This change checks catalogue cardinality, repeat-run idempotence and reservation preservation against PostgreSQL. It does not establish a production uptime or load-capacity guarantee. A catalogue of 750 records is demo data, not a load test.

Before deployment, still needed:

- Browser verification of the refreshed screens on desktop and mobile.
- Admin property/room/inventory management, using compatible transaction locking.
- Auth rate limiting, email verification and password recovery for a public account launch.
- External HTTPS hosting, private secrets and a least-privilege database role.
- Database backup/restore drill, monitoring, health checks and error reporting.
- Shared session storage before running multiple application instances; current sessions are in memory.
- Hosting failover or multiple instances plus database recovery arrangements for actual redundancy.
- Realistic traffic/concurrency tests and measured resource limits.

There is no deployment in this PR. The hosted static preview is unchanged. Local users need the updated project files and the demo startup command above.
