-- Run only in a disposable test database AFTER V1. Fixtures roll back.
BEGIN;
CREATE FUNCTION pg_temp.expect_failure(statement TEXT, expected_state TEXT)
RETURNS VOID LANGUAGE plpgsql AS $$
BEGIN
    BEGIN
        EXECUTE statement;
    EXCEPTION WHEN OTHERS THEN
        IF SQLSTATE = expected_state THEN RETURN; END IF;
        RAISE;
    END;
    RAISE EXCEPTION 'Expected SQLSTATE %, but statement succeeded: %', expected_state, statement;
END;
$$;

DO $$
DECLARE
    customer BIGINT; property_a BIGINT; property_b BIGINT;
    deluxe BIGINT; foreign_room BIGINT; reservation BIGINT;
BEGIN
    INSERT INTO users(full_name, email, password_hash)
        VALUES ('Schema fixture', 'schema-check@example.invalid', 'TEST-ONLY-NOT-A-LOGIN-HASH') RETURNING id INTO customer;
    INSERT INTO properties(name, property_type, city, address)
        VALUES ('Fixture A', 'HOTEL', 'Goa', 'Test address') RETURNING id INTO property_a;
    INSERT INTO properties(name, property_type, city, address)
        VALUES ('Fixture B', 'VILLA', 'Goa', 'Test address') RETURNING id INTO property_b;
    INSERT INTO room_types(property_id, name, capacity, total_quantity, base_nightly_price)
        VALUES (property_a, 'Deluxe', 2, 10, 4000) RETURNING id INTO deluxe;
    INSERT INTO room_types(property_id, name, capacity, total_quantity, base_nightly_price)
        VALUES (property_b, 'Entire villa', 4, 1, 6000) RETURNING id INTO foreign_room;
    INSERT INTO room_inventory VALUES (deluxe, DATE '2030-10-10', 10, 6);
    INSERT INTO room_inventory VALUES (deluxe, DATE '2030-10-11', 10, 8);
    INSERT INTO room_inventory VALUES (deluxe, DATE '2030-10-12', 10, 5);
    IF (SELECT min(sellable_quantity-reserved_quantity) FROM room_inventory WHERE room_type_id=deluxe) <> 2 THEN
        RAISE EXCEPTION 'Expected minimum availability of two';
    END IF;
    INSERT INTO bookings(reference,user_id,property_id,checkin,checkout,guest_count,total_amount,cancellation_deadline,accepted_policy)
        VALUES ('SCHEMA-TEST',customer,property_a,'2030-10-10','2030-10-13',4,24000,
                '2030-10-10 00:00:00+05:30','Free cancellation before the check-in date') RETURNING id INTO reservation;
    INSERT INTO booking_items(booking_id,property_id,room_type_id,quantity,agreed_nightly_price,subtotal)
        VALUES (reservation,property_a,deluxe,2,4000,24000);
    -- Changing the catalogue rate must not change an existing price snapshot.
    UPDATE room_types SET base_nightly_price=5000 WHERE id=deluxe;
    IF (SELECT agreed_nightly_price FROM booking_items WHERE booking_id=reservation) <> 4000 THEN
        RAISE EXCEPTION 'Booked price changed';
    END IF;
    PERFORM pg_temp.expect_failure($q$INSERT INTO users(full_name,email,password_hash) VALUES ('Other','SCHEMA-CHECK@example.invalid','test')$q$,'23505');
    PERFORM pg_temp.expect_failure(format('UPDATE room_inventory SET reserved_quantity=11 WHERE room_type_id=%s',deluxe),'23514');
    PERFORM pg_temp.expect_failure(format('UPDATE room_inventory SET reserved_quantity=-1 WHERE room_type_id=%s',deluxe),'23514');
    PERFORM pg_temp.expect_failure(format('INSERT INTO room_inventory VALUES (%s,DATE ''2030-10-10'',10,0)',deluxe),'23505');
    PERFORM pg_temp.expect_failure(format('UPDATE bookings SET checkout=checkin WHERE id=%s',reservation),'23514');
    PERFORM pg_temp.expect_failure(format('UPDATE bookings SET guest_count=0 WHERE id=%s',reservation),'23514');
    PERFORM pg_temp.expect_failure(format('UPDATE bookings SET status=''UNKNOWN'' WHERE id=%s',reservation),'23514');
    PERFORM pg_temp.expect_failure(format('UPDATE room_types SET base_nightly_price=-1 WHERE id=%s',deluxe),'23514');
    PERFORM pg_temp.expect_failure(format('UPDATE room_types SET base_nightly_price=''NaN'' WHERE id=%s',deluxe),'23514');
    PERFORM pg_temp.expect_failure(format('UPDATE booking_items SET room_type_id=%s WHERE booking_id=%s',foreign_room,reservation),'23503');
    PERFORM pg_temp.expect_failure(format('UPDATE booking_items SET property_id=%s WHERE booking_id=%s',property_b,reservation),'23503');
    PERFORM pg_temp.expect_failure(format('UPDATE booking_items SET quantity=0 WHERE booking_id=%s',reservation),'23514');
    PERFORM pg_temp.expect_failure(format('INSERT INTO booking_items(booking_id,property_id,room_type_id,quantity,agreed_nightly_price,subtotal) VALUES (%s,%s,%s,1,4000,12000)',reservation,property_a,deluxe),'23505');
    PERFORM pg_temp.expect_failure(format('DELETE FROM users WHERE id=%s',customer),'23503');
    RAISE NOTICE 'PASS: valid fixture, availability, price snapshot and 14 rejection checks';
END;
$$;
ROLLBACK;
