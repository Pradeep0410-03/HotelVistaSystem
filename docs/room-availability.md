# Room types, prices and availability

## This phase

Open a database property, choose check-in/check-out, guests and room quantity, then check availability. Spring returns suitable room types with the minimum quantity available throughout the stay and a server-calculated subtotal. This is a read-only check; it does not hold inventory or create bookings.

GET /api/properties/{id}/availability?checkin=YYYY-MM-DD&checkout=YYYY-MM-DD&guests=2&rooms=1

The public endpoint accepts 1–100 guests, 1–20 rooms, and 1–30 nights. Check-in cannot precede today in the property's timezone; check-out must be within 365 days of that date. It returns 400 for invalid input and 404 for inactive/missing properties.

Each returned option satisfies the complete request using one room type. Mixed room-type bookings are not offered in this phase. An entire villa can be modelled as a room type named Entire villa with total quantity 1.

## Rules

- Check-in is inclusive, check-out exclusive.
- Missing inventory for any requested night makes that room type unavailable.
- Available quantity is the minimum across nights of min(sellable quantity, total room quantity) minus reserved quantity.
- Requested guests must fit room capacity multiplied by requested rooms.
- Only active properties and room types are returned.
- Base nightly price is currently constant across the stay. BigDecimal computes price × nights × room quantity. INR subtotal excludes taxes and fees.
- All hotels share pay-at-hotel and free cancellation before the check-in date in local time; subsequent cancellation requests require admin review. The response supplies the exact midnight deadline. Booking creation must later snapshot the policy and deadline.
- This read can become stale immediately. The future booking transaction must lock/recheck inventory and prices before confirming; this endpoint alone does not prevent concurrent overbooking.

## Code walkthrough

AvailabilityController validates query fields. AvailabilityService checks dates against the property's timezone and computes subtotals. AvailabilityRepository uses Spring JdbcTemplate with parameterized SQL for the grouped nightly query. Existing property/account persistence stays on JPA; the aggregate is expressed directly in SQL so the per-night rule is easy to inspect. No schema migration is needed.

The property client calls this API. The detail form renders room options, subtotal and cancellation deadline. It never adds sample rooms to an empty response and offers no confirmation button yet.

## Local demonstration

Use a disposable database and the Spring local setup in backend/README.md. This optional SQL creates one clearly named demo property and Deluxe inventory for the next 30 days. Run once; repeated runs create another demo property. It is not an application startup seed.

```sql
WITH property AS (
  INSERT INTO properties(name,property_type,city,address)
  VALUES ('Availability Demo','HOTEL','Goa','Local demo only') RETURNING id
), room AS (
  INSERT INTO room_types(property_id,name,capacity,total_quantity,base_nightly_price)
  SELECT id,'Deluxe',2,10,2500.00 FROM property RETURNING id
)
INSERT INTO room_inventory(room_type_id,stay_date,sellable_quantity,reserved_quantity)
SELECT room.id, (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Kolkata')::date + day, 10, 0
FROM room CROSS JOIN generate_series(0,29) AS day;
```

Open the homepage through Spring, search Goa and open Availability Demo. A two-night stay with two rooms should show INR 10,000 subtotal. Nothing is booked. The hosted static preview remains unchanged.

## Verification

AvailabilityIT runs against PostgreSQL in the Maven integration profile. It covers the limiting night, capacity, precise subtotal, exclusive checkout, missing nights, inactive records and invalid input. Run node --test tests/*.test.cjs for client checks. No browser visual testing was performed in this phase.
