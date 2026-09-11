CREATE TABLE movies (
 id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 title VARCHAR(180) NOT NULL CHECK (btrim(title) <> ''),
 language VARCHAR(40) NOT NULL CHECK (btrim(language) <> ''),
 duration_minutes INTEGER NOT NULL CHECK (duration_minutes BETWEEN 1 AND 600),
 active BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE TABLE cinemas (
 id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 name VARCHAR(180) NOT NULL CHECK (btrim(name) <> ''),
 city VARCHAR(100) NOT NULL CHECK (btrim(city) <> ''),
 address VARCHAR(500) NOT NULL CHECK (btrim(address) <> ''),
 timezone VARCHAR(64) NOT NULL DEFAULT 'Asia/Kolkata'
);
CREATE TABLE movie_shows (
 id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 movie_id BIGINT NOT NULL REFERENCES movies(id),
 cinema_id BIGINT NOT NULL REFERENCES cinemas(id),
 starts_at TIMESTAMPTZ NOT NULL CHECK (isfinite(starts_at)),
 active BOOLEAN NOT NULL DEFAULT TRUE,
 demo_key VARCHAR(100) UNIQUE
);
CREATE INDEX movie_shows_upcoming_idx ON movie_shows(movie_id,starts_at) WHERE active;
CREATE TABLE movie_bookings (
 id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 reference VARCHAR(40) NOT NULL UNIQUE,
 user_id BIGINT NOT NULL REFERENCES users(id),
 show_id BIGINT NOT NULL REFERENCES movie_shows(id),
 request_key UUID NOT NULL,
 request_fingerprint TEXT NOT NULL,
 total_amount NUMERIC(12,2) NOT NULL CHECK (total_amount >= 0 AND total_amount <> 'NaN'::numeric),
 currency CHAR(3) NOT NULL DEFAULT 'INR' CHECK (currency = 'INR'),
 status VARCHAR(16) NOT NULL DEFAULT 'CONFIRMED' CHECK (status IN ('CONFIRMED','CANCELLED')),
 accepted_policy TEXT NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE(user_id,request_key), UNIQUE(id,show_id)
);
CREATE INDEX movie_bookings_owner_idx ON movie_bookings(user_id,id DESC);
CREATE TABLE movie_show_seats (
 show_id BIGINT NOT NULL REFERENCES movie_shows(id),
 label VARCHAR(8) NOT NULL CHECK (label ~ '^[A-Z][1-9][0-9]{0,2}$'),
 price NUMERIC(10,2) NOT NULL CHECK (price >= 0 AND price <> 'NaN'::numeric),
 booking_id BIGINT,
 PRIMARY KEY(show_id,label),
 FOREIGN KEY(booking_id,show_id) REFERENCES movie_bookings(id,show_id)
);
CREATE TABLE movie_booking_seats (
 booking_id BIGINT NOT NULL,
 show_id BIGINT NOT NULL,
 label VARCHAR(8) NOT NULL,
 agreed_price NUMERIC(10,2) NOT NULL CHECK (agreed_price >= 0 AND agreed_price <> 'NaN'::numeric),
 PRIMARY KEY(booking_id,label),
 FOREIGN KEY(booking_id,show_id) REFERENCES movie_bookings(id,show_id),
 FOREIGN KEY(show_id,label) REFERENCES movie_show_seats(show_id,label)
);
