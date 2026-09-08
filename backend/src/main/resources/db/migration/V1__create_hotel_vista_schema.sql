-- PostgreSQL schema, ready for Flyway when Spring Boot is introduced.
-- No accounts, passwords, sample listings or reservations are seeded here.
CREATE TABLE users (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL CHECK (btrim(full_name) <> ''),
    email VARCHAR(254) NOT NULL CHECK (email = btrim(email) AND email <> ''),
    password_hash VARCHAR(255) NOT NULL CHECK (btrim(password_hash) <> ''),
    role VARCHAR(16) NOT NULL DEFAULT 'CUSTOMER' CHECK (role IN ('CUSTOMER', 'ADMIN')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
-- Email syntax and ownership verification belong in the account service.
CREATE UNIQUE INDEX users_email_unique ON users (lower(email));

CREATE TABLE properties (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(180) NOT NULL CHECK (btrim(name) <> ''),
    property_type VARCHAR(24) NOT NULL CHECK (property_type IN
        ('HOTEL', 'RESORT', 'APARTMENT', 'HOMESTAY', 'VILLA', 'FARMHOUSE', 'BUNGALOW')),
    city VARCHAR(100) NOT NULL CHECK (btrim(city) <> ''),
    address TEXT NOT NULL CHECK (btrim(address) <> ''),
    description TEXT NOT NULL DEFAULT '',
    timezone VARCHAR(64) NOT NULL DEFAULT 'Asia/Kolkata' CHECK (btrim(timezone) <> ''),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX properties_city_active_idx ON properties (lower(city)) WHERE active;

CREATE TABLE room_types (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    property_id BIGINT NOT NULL REFERENCES properties(id),
    name VARCHAR(100) NOT NULL CHECK (btrim(name) <> ''),
    capacity INTEGER NOT NULL CHECK (capacity > 0),
    total_quantity INTEGER NOT NULL CHECK (total_quantity > 0),
    base_nightly_price NUMERIC(12,2) NOT NULL
        CHECK (base_nightly_price >= 0 AND base_nightly_price <> 'NaN'::numeric),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE (id, property_id),
    UNIQUE (property_id, name)
);

CREATE TABLE room_inventory (
    room_type_id BIGINT NOT NULL REFERENCES room_types(id),
    stay_date DATE NOT NULL CHECK (isfinite(stay_date)),
    sellable_quantity INTEGER NOT NULL CHECK (sellable_quantity >= 0),
    reserved_quantity INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (room_type_id, stay_date),
    CONSTRAINT inventory_quantity_bounds CHECK
        (reserved_quantity >= 0 AND reserved_quantity <= sellable_quantity)
);
-- Available quantity is derived: sellable_quantity - reserved_quantity.
-- A missing row means unavailable, not unlimited availability.

CREATE TABLE bookings (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    reference VARCHAR(40) NOT NULL UNIQUE CHECK (btrim(reference) <> ''),
    user_id BIGINT NOT NULL REFERENCES users(id),
    property_id BIGINT NOT NULL REFERENCES properties(id),
    checkin DATE NOT NULL CHECK (isfinite(checkin)),
    checkout DATE NOT NULL CHECK (isfinite(checkout)),
    guest_count INTEGER NOT NULL CHECK (guest_count > 0),
    status VARCHAR(16) NOT NULL DEFAULT 'CONFIRMED'
        CHECK (status IN ('CONFIRMED', 'CANCELLED', 'COMPLETED')),
    total_amount NUMERIC(14,2) NOT NULL
        CHECK (total_amount >= 0 AND total_amount <> 'NaN'::numeric),
    currency CHAR(3) NOT NULL DEFAULT 'INR' CHECK (currency = 'INR'),
    cancellation_deadline TIMESTAMPTZ NOT NULL CHECK (isfinite(cancellation_deadline)),
    accepted_policy TEXT NOT NULL CHECK (btrim(accepted_policy) <> ''),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT booking_date_order CHECK (checkout > checkin),
    UNIQUE (id, property_id)
);
CREATE INDEX bookings_user_created_idx ON bookings (user_id, created_at DESC);
CREATE INDEX bookings_property_checkin_idx ON bookings (property_id, checkin);

CREATE TABLE booking_items (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    booking_id BIGINT NOT NULL,
    property_id BIGINT NOT NULL,
    room_type_id BIGINT NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    agreed_nightly_price NUMERIC(12,2) NOT NULL
        CHECK (agreed_nightly_price >= 0 AND agreed_nightly_price <> 'NaN'::numeric),
    subtotal NUMERIC(14,2) NOT NULL CHECK (subtotal >= 0 AND subtotal <> 'NaN'::numeric),
    UNIQUE (booking_id, room_type_id),
    -- Both parents must match the SAME property_id.
    CONSTRAINT item_booking_property_fk FOREIGN KEY (booking_id, property_id)
        REFERENCES bookings(id, property_id),
    CONSTRAINT item_room_property_fk FOREIGN KEY (room_type_id, property_id)
        REFERENCES room_types(id, property_id)
);
CREATE INDEX booking_items_room_property_idx ON booking_items (room_type_id, property_id);
-- No cascading deletes: retain reservation history; deactivate catalogue records.
-- Booking totals, price snapshots, state transitions, inventory consistency and
-- guest capacity across items must be coordinated by the transactional service.
