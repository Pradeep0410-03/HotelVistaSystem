# Hotel Vista Spring Boot foundation

This phase provides a read-only property API with Spring Boot 3.5.16, Java 17, Spring Data JPA, PostgreSQL and Flyway. It builds on V1 from the database phase. No login, booking or administration API exists yet; the hosted frontend continues to use its sample catalogue.

## 1. Install prerequisites

Install JDK 17 or newer (17 is the compilation target), Maven 3.6.3 or newer, and PostgreSQL with `psql`/`createdb`. Verify `java -version`, `mvn -version`, and `psql --version`. No IDE plugin or new connector is required. Open `backend/pom.xml` as a Maven project in your Java IDE.

## 2. Prepare a fresh local database

```sh
createdb hotelvista_dev
```

Use a PostgreSQL role with permission to create tables in that database. For this phase Spring uses that role for Flyway and reads; separate runtime and migration privileges are a later deployment step.

If you manually applied V1 in the previous phase, create a new empty database such as `hotelvista_spring_dev` and point DB_URL to it. Do not drop existing data or enable automatic baselining to hide missing migration history.

## 3. Set connection values

Spring reads environment variables, not a `.env` file automatically. Set these in your IDE run configuration or terminal:

| Variable | Value |
|---|---|
| DB_URL | `jdbc:postgresql://localhost:5432/hotelvista_dev` (default) |
| DB_USERNAME | Your local PostgreSQL role (required) |
| DB_PASSWORD | That role's password (required; explicitly empty only if local auth permits it) |
| PORT | `8080` (default) |

Do not commit passwords or put them into JDBC URLs. For hosted PostgreSQL later, follow its certificate/TLS requirements. None is provisioned here.

## 4. Start the backend

From `backend/`:

```sh
mvn spring-boot:run
```

Flyway creates the six tables and records V1 in `flyway_schema_history`. Hibernate validates the entity mapping; it does not create or alter the schema. Application startup requires a reachable database. The five-connection pool is a small initial development setting, not a capacity guarantee.

Open `http://localhost:8080/api/properties` in your browser. A fresh database correctly returns:

```json
{"items":[],"page":0,"size":12,"hasNext":false}
```

## 5. Optional local sample property

Only in a disposable/local development database, run this once using psql:

```sql
INSERT INTO properties(name, property_type, city, address, description)
VALUES ('Palm & Tide Demo', 'RESORT', 'Goa', 'Demo address, North Goa',
        'Sample listing for learning the property API');
```

The generated ID is database-owned and need not be 1. This listing has no room inventory or real availability. No demo data or users are created automatically in the application.

## API contract

| Request | Behavior |
|---|---|
| `GET /api/properties` | Active listings, stable ID order, 12 per page |
| `GET /api/properties?city=Goa&page=0&size=12` | Trimmed, case-insensitive exact city match |
| `GET /api/properties/{id}` | Active listing or 404 |

`page` is zero-based, limited to 0-10000; `size` is 1-50. Empty/whitespace city means all active properties. City supports up to 100 characters; it does not resolve aliases or perform substring search yet. Bad numbers and out-of-range input return 400. A slice gives `hasNext` without a total count. No price, room availability or booking claim is returned.

Catalogue GET routes are intentionally public. Only these reads are implemented; no credentials or user records are exposed. Spring Security and CSRF-protected state-changing endpoints belong to the next account phase. No permissive cross-origin configuration is added. Visiting the API in the browser works; connecting the hosted frontend needs a deliberate same-origin/proxy or limited CORS setup later.

## Verification

```sh
mvn test
mvn package
```

Controller checks run without a database. They verify response shape, validation and refusal of POST requests. To verify the full controller/service/JPA/Flyway path, set DB_URL, DB_USERNAME and DB_PASSWORD to a fresh disposable PostgreSQL database, then run:

```sh
mvn -Pintegration verify
```

Integration tests migrate the schema and roll back their test fixtures; schema history and identity sequence increments remain. They require a real reachable PostgreSQL-compatible service, do not silently skip when it is unavailable, and do not use H2. Never point them at a production database.

The packaged application is `target/hotel-vista-api-0.1.0-SNAPSHOT.jar`, runnable with `java -jar` and the same environment variables.

Read [the request walkthrough](../docs/spring-foundation.md) next. Existing empty JavaScript backend placeholders are historical files, not part of this Maven application.

Reference: [Spring Boot 3.5 requirements](https://docs.spring.io/spring-boot/3.5/system-requirements.html).
