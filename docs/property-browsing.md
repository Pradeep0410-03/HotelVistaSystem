# Database property browsing

This phase connects the homepage to GET /api/properties and GET /api/properties/{id}. It uses PostgreSQL records through the existing controller, service and repository. No schema change is needed.

## Try it

Follow backend/README.md to start Spring with PostgreSQL, then visit http://localhost:8080/index.html. The default view lists active database properties. Search by the complete city name (case-insensitive); blank input lists all cities. Previous and Next fetch pages of twelve records. View details fetches that property again, so a removed listing can report that it is no longer available.

A new database has no listings. The empty state is expected. The optional local sample INSERT in backend/README.md adds a clearly named demo record; search Goa to retrieve it. Records are never inserted automatically. Production data should eventually be managed through the admin interface.

## Separate sample experience

Explore sample booking flow opens the existing static catalogue, filters, prices, saved stays and trip/login flow. Existing destination and property links open that sample view. Those sample IDs and prices are never merged with database records. Use Browse properties to return to database results.

Database listings currently have no photos, nightly prices, room inventory or availability. The view shows only property type, name, city, address, description and timezone. It does not present a Reserve button or claim the property is available for dates. Room availability and booking are separate phases.

An unavailable API shows an error and retry control; it does not silently replace results with samples. The hosted Sites preview still has no Java backend. This PR does not deploy it.

## Read the flow

1. index.html supplies the city form, results area and catalogue switch.
2. js/database-browse.js handles loading, empty/error states, pagination and details. New searches cancel old requests; late responses cannot replace current results. API text is rendered with textContent.
3. js/property-client.js validates inputs, encodes the city and checks the API response shape.
4. PropertyController -> PropertyService -> PropertyRepository queries active database records and returns a bounded page.

Run node --test tests/*.test.cjs and the PostgreSQL Maven integration workflow. Client tests cover encoded city queries, pagination, empty results, malformed/static responses and removed details. Existing PropertyApiIT verifies database filtering, pagination and inactive-listing protection. Browser visual QA is not part of this phase.
