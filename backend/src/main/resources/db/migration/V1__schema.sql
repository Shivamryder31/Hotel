-- Hotels: one room category per hotel, `total_rooms` is the physical capacity.
CREATE TABLE hotels (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    name            VARCHAR(120)  NOT NULL,
    location        VARCHAR(120)  NOT NULL,
    description     TEXT          NOT NULL,
    price_per_night DECIMAL(10,2) NOT NULL,
    total_rooms     INT           NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT chk_hotels_price CHECK (price_per_night > 0),
    CONSTRAINT chk_hotels_rooms CHECK (total_rooms > 0),
    INDEX idx_hotels_location (location)
) ENGINE=InnoDB;

CREATE TABLE hotel_amenities (
    hotel_id BIGINT       NOT NULL,
    amenity  VARCHAR(60)  NOT NULL,
    PRIMARY KEY (hotel_id, amenity),
    CONSTRAINT fk_amenities_hotel FOREIGN KEY (hotel_id) REFERENCES hotels (id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- One row per hotel per night: how many rooms are reserved on that night.
-- A stay of [check_in, check_out) reserves one unit on each night it covers;
-- the check-out day itself is NOT reserved, so back-to-back stays never clash.
CREATE TABLE room_inventory (
    hotel_id     BIGINT NOT NULL,
    stay_date    DATE   NOT NULL,
    booked_rooms INT    NOT NULL DEFAULT 0,
    PRIMARY KEY (hotel_id, stay_date),
    CONSTRAINT fk_inventory_hotel FOREIGN KEY (hotel_id) REFERENCES hotels (id) ON DELETE CASCADE,
    CONSTRAINT chk_inventory_booked CHECK (booked_rooms >= 0)
) ENGINE=InnoDB;

CREATE TABLE bookings (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    reference       VARCHAR(16)   NOT NULL,
    hotel_id        BIGINT        NOT NULL,
    guest_name      VARCHAR(100)  NOT NULL,
    guest_email     VARCHAR(254)  NOT NULL,
    check_in        DATE          NOT NULL,
    check_out       DATE          NOT NULL,
    nights          INT           NOT NULL,
    price_per_night DECIMAL(10,2) NOT NULL,   -- snapshot of the price at booking time
    total_amount    DECIMAL(12,2) NOT NULL,
    status          VARCHAR(12)   NOT NULL,
    created_at      DATETIME(6)   NOT NULL,
    cancelled_at    DATETIME(6)   NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_bookings_reference (reference),
    CONSTRAINT fk_bookings_hotel FOREIGN KEY (hotel_id) REFERENCES hotels (id),
    CONSTRAINT chk_bookings_dates CHECK (check_out > check_in),
    CONSTRAINT chk_bookings_status CHECK (status IN ('CONFIRMED', 'CANCELLED')),
    INDEX idx_bookings_hotel_dates (hotel_id, check_in, check_out)
) ENGINE=InnoDB;
