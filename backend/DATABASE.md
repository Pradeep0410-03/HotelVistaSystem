# Database phase: PostgreSQL

For the Spring application, start with [backend setup](README.md): Flyway now applies V1 at startup to a fresh database. The manual commands below remain useful for isolated schema exercises.

Start with [the diagram and SQL walkthrough](../docs/database-design.md).

The first migration lives at `src/main/resources/db/migration/V1__create_hotel_vista_schema.sql`. This follows Flyway naming for the future Spring project; Flyway is configured by the Spring foundation; the commands below are the alternative manual route for a separate database. There are no real credentials or seed users in the migration.

## Run locally once PostgreSQL is installed

From the repository root, using an account allowed to create databases:

```sh
createdb hotelvista_dev
psql -X -v ON_ERROR_STOP=1 --single-transaction -d hotelvista_dev -f backend/src/main/resources/db/migration/V1__create_hotel_vista_schema.sql
```

Use a new, empty database. Do not apply V1 twice. For an application database, choose either this manual setup for this phase or a fresh database managed by Flyway once integrated; manually applied SQL is not automatically recorded in Flyway history. Do not put database passwords in committed configuration or shell command arguments.

## Verify constraints in a separate test database

```sh
createdb hotelvista_schema_test
psql -X -v ON_ERROR_STOP=1 --single-transaction -d hotelvista_schema_test -f backend/src/main/resources/db/migration/V1__create_hotel_vista_schema.sql
psql -X -v ON_ERROR_STOP=1 -d hotelvista_schema_test -f backend/tests/schema_checks.sql
```

The test inserts disposable fixtures, checks a valid booking, minimum nightly availability and preservation of an accepted rate, then checks 14 invalid operations are rejected. Its transaction rolls back its fixtures; identity sequences may still advance. It is intended for a disposable database, not live customer data. The test user has an intentionally unusable placeholder hash, not a login credential.

Checks include duplicate email with changed case, overselling, negative inventory, duplicate nights, same-day checkout, invalid guests/status/prices including NaN, cross-property items, zero quantity, duplicate booking items and deleting a referenced customer.

## Validation performed for this change

Executed the migration and SQL checks using PGlite's embedded PostgreSQL engine in a temporary local validation environment. Confirmed six tables and no retained user fixture after rollback. A standalone PostgreSQL server, Flyway integration and multi-connection concurrency tests have not been run here. The commands above are the repeatable standalone PostgreSQL verification step.

The integrated Spring application now connects frontend browsing and availability to PostgreSQL. External database hosting is not provisioned here; see README.md for setup and current integration checks.
