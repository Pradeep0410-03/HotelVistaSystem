# Follow one request through Hotel Vista

Example: `GET /api/properties?city=Goa`.

1. **PropertyController** receives HTTP input. Validation rejects an oversized city or invalid page size before calling the service.
2. **PropertyService** trims city input and sets a stable page order. A read-only transaction covers fetching and response mapping.
3. **PropertyRepository** uses a parameterized JPA query to select active listings. User input is a bound value, never pasted into SQL.
4. **Hibernate and JDBC** translate the query and communicate with PostgreSQL through the connection pool.
5. **PropertyResponse / PropertyPage** return an explicit JSON shape. Entities and database connection details are not exposed.

For detail requests, both a missing ID and an inactive property return 404. This prevents inactive listings from leaking through direct URLs. The error advice produces 400/404 problem responses without echoing arbitrary input or database messages.

## File map

| File under backend/ | Why it exists |
|---|---|
| pom.xml | Java version, dependencies and build/test plugins |
| src/main/resources/application.yml | Environment-based database config, Flyway and Hibernate validation |
| src/main/resources/db/migration/V1__create_hotel_vista_schema.sql | Existing migration, unchanged |
| src/main/java/com/hotelvista/HotelVistaApplication.java | Starts Spring and scans its packages |
| property/Property.java (under the Java package) | Maps the properties table to an entity |
| property/PropertyRepository.java | Database reads only; no exposed save/delete methods |
| property/PropertyService.java | Read workflow and entity-to-response conversion |
| property/PropertyController.java | HTTP endpoints and request validation |
| property/PropertyResponse.java, PropertyPage.java | Public response records |
| common/ApiErrors.java | Validation and not-found errors |
| src/test/java/com/hotelvista/PropertyControllerTest.java | Database-free HTTP checks |
| src/test/java/com/hotelvista/PropertyApiIT.java | Opt-in integration checks against PostgreSQL |

## Why Flyway and JPA both exist

Flyway creates and evolves tables using reviewed SQL files. JPA maps selected tables into Java objects so services can query them. `ddl-auto: validate` checks compatibility; it does not replace migrations. Only the properties table is mapped in this phase; other entities follow when their features are built.

## Practice

Start the application against an empty database and see an empty JSON list. Add the sample property from backend/README.md and repeat the request. Search another city and get an empty list. Request a missing ID and get 404. Ask for `size=0` and get 400.

This is the first database-backed API, not live room search: check-in, checkout, capacity, prices and nightly availability will be added in the catalogue/booking phases. The site preview will look unchanged until the frontend is deliberately connected.

The Mockito test configuration uses its subclass mock maker because these tests mock a normal service class, not final/static methods. It avoids requiring JVM self-attachment in restricted development environments. This setting applies only to tests.

GitHub's backend workflow runs the same integration profile with a disposable PostgreSQL 17 service. Its sample credentials are for that temporary test service only. They are not production secrets or application defaults.
