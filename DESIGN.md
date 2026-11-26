# Design write-up

## 1. Architecture

```
Angular 18 (SPA, :4200)  --HTTP/JSON-->  Spring Boot 3 REST API (:8080)  --JPA/Hibernate-->  MySQL 8
   pages + ApiService                     controller -> service -> repository                 Flyway-managed schema
```

- **Frontend** talks only to `/api/**`. In dev, `proxy.conf.json` forwards to the backend (no CORS needed; CORS is also configured for `localhost:4200`).
- **Backend** layers: `web` (controllers, DTOs, global error handler) -> `service` (booking rules, availability, pricing) -> `repository` (Spring Data JPA) -> `domain` (entities, `StayPeriod`). Controllers contain no business logic; entities are never returned directly (DTO records are).
- **Database** schema and sample data are versioned Flyway migrations; Hibernate does not alter the schema (`ddl-auto: none`).
- Credentials come from environment variables; nothing secret is committed.

## 2. Database design

| Table | Purpose | Key constraints |
|---|---|---|
| `hotels` | Hotel info, nightly price, `total_rooms` capacity | `price > 0`, `total_rooms > 0` |
| `hotel_amenities` | Amenities (one row each) | PK `(hotel_id, amenity)`, FK -> hotels |
| `room_inventory` | Rooms reserved per hotel **per night** | PK `(hotel_id, stay_date)`, `booked_rooms >= 0`, FK |
| `bookings` | A guest's stay | UNIQUE `reference`, FK hotel, `check_out > check_in`, status in (`CONFIRMED`,`CANCELLED`), index `(hotel_id, check_in, check_out)` |

Relationships: hotel 1—N amenities, hotel 1—N inventory rows, hotel 1—N bookings.

Why a per-night inventory table? It makes "availability for a stay" a simple min over nights, makes overlap handling
explicit, and gives the assignment's "persist room availability" a concrete home. Rows are created lazily the first
time a night is booked (no row = nothing booked). Bookings keep a **snapshot** of price/night and total so price changes never rewrite history.
Cancelled bookings are kept (status flag) for auditability instead of being deleted.

## 3. API design

See the table in the README. Notable choices:
- `GET /hotels/{id}/quote` exists so the UI can show the total *before* confirming while the calculation stays on the backend; `POST /bookings` never accepts a price.
- `POST /bookings/{ref}/cancel` is a POST with the email in the body (not DELETE) because the email is a credential-like value and shouldn't be in a URL; lookup uses `GET ...?email=` for simplicity (trade-off noted below).
- Status codes: `201` created, `400` validation / bad dates, `404` unknown hotel or booking, `409` sold out / already cancelled / cancellation not allowed, `500` unexpected (generic message, details only in logs).
- Every error uses `ApiError { timestamp, status, code, message, path, fieldErrors? }`; the UI shows `message` and maps `fieldErrors` onto inputs.

## 4. Booking logic

- **Dates** (`StayPeriod.of`): both required; check-in not in the past; check-out after check-in; max 30 nights. The frontend repeats these checks only for instant feedback.
- **Pricing** (`PriceCalculator`): `pricePerNight x nights`, scale 2, `BigDecimal` throughout (no floating point).
- **Availability**: for each night in `[check-in, check-out)`, `free = total_rooms - booked_rooms`; the stay's availability is the minimum over its nights. The check-out day is not an occupied night, so back-to-back stays are allowed.
- **Create**: validate -> lock hotel row -> load/create inventory rows -> reject if any night is full (`409`) -> increment all nights -> insert booking with a random reference (`HB-` + 8 chars from an unambiguous alphabet, uniqueness enforced by a DB constraint) — all in one transaction.
- **Cancel**: look up by reference + email with a row lock -> reject if already cancelled or the stay has started -> lock hotel -> decrement each night's counter -> mark `CANCELLED`. The room is immediately bookable again.

## 5. Design decisions, alternatives and trade-offs

| Decision | Alternative considered | Why |
|---|---|---|
| Per-night inventory counters | Compute availability by counting overlapping bookings (`check_in < :out AND check_out > :in`) | Counting is simpler and has no derived state to drift, but it can't be locked per night and "persisted availability" was requested. Counters trade a bit of redundancy for explicit, cheap checks. |
| Pessimistic lock on the hotel row | Optimistic locking with retries; `SERIALIZABLE`; conditional `UPDATE ... WHERE booked_rooms < total` per night | See section 6. |
| Flyway SQL migrations | `ddl-auto=update`, `schema.sql` | Reviewable, repeatable, matches the "database scripts" requirement. |
| DTO records, one error shape | Returning entities, ad-hoc errors | Stable API contract, no lazy-loading/serialisation surprises. |
| Amenities in their own table | Comma-separated string / JSON column | Queryable and normalised; slightly more joins (fine at this scale). |
| Angular standalone components + plain CSS | Angular Material, NgRx | Few screens; avoids dependencies and boilerplate. |
| Reference + email as the "credential" | Accounts | Required by the brief; a wrong email and a wrong reference give the same error so references can't be enumerated. |

## 6. Simultaneous bookings (two users, last room)

`BookingService.createBooking` runs in one transaction that first does `SELECT ... FOR UPDATE` on the **hotel row**
(`HotelRepository.findByIdForUpdate`). Concurrent bookings and cancellations for the same hotel therefore queue up:

1. User A and B both request the last room. A gets the lock, sees `booked < total`, increments the nights, inserts the booking, commits.
2. B was blocked on the hotel row; once A commits, B reads the *new* counters (locking reads see the latest committed data), finds the night full and receives `409 ROOM_UNAVAILABLE`. The UI then refreshes availability and shows the flexible-date suggestions.

Bookings for *different* hotels don't block each other. The cost is that two bookings for different dates at the same hotel are also serialised — acceptable at this scale because the critical section is a handful of small queries.

Cancellation takes the booking-row lock first and then the hotel lock (create takes only the hotel lock), so lock order is consistent and a double-click on "cancel" can't release the room twice.

**At larger scale** I would switch to finer-grained locking (lock only the affected `room_inventory` rows in date order), or an atomic `UPDATE room_inventory SET booked_rooms = booked_rooms + 1 WHERE hotel_id=? AND stay_date=? AND booked_rooms < ?` per night inside the transaction (rolling back if any row updates 0), plus a short retry on deadlock/lock-timeout.

## 7. Surprise feature — Flexible-date suggestions

- **What:** When the chosen dates are sold out, the hotel page shows up to three alternative stays of the *same length* within ±7 days that still have a room, ordered by closeness, each with its total price. Clicking one switches the dates and refreshes the quote.
- **Why:** A sold-out result is where most users abandon a booking. People with flexible travel plans would rather see "Thu–Sat is free" than guess dates one at a time. It's small, but it turns a dead end into a next step.
- **How:** `GET /api/hotels/{id}/alternatives` -> `AvailabilityService.suggestAlternatives` loads the hotel's inventory rows for the whole ±7-day window with **one query**, then reuses the same pure `computeAvailable` function used for normal availability to test each shifted candidate (skipping check-in dates in the past). The frontend calls it only when the quote reports zero rooms, and also re-fetches it if a booking attempt loses a race (`409`).

## 8. Limitations and what I'd improve

- No automated test against real MySQL: the concurrency guarantee relies on row locks that unit tests with mocks can't prove. Next step: Testcontainers-based integration test firing parallel booking requests for the last room.
- Booking lookup passes the email as a query parameter (could appear in access logs); with more time, use a POST lookup or a signed manage-link emailed to the guest. No actual email is sent.
- No rate limiting on lookup/cancel (reference + email can be brute-forced in theory; the 8-char reference has ~40 bits of entropy).
- One room category per hotel; no multi-room bookings, rate plans, taxes, seasonal pricing or payment.
- Search is a simple `LIKE` on location; hotel-name/amenity filters, sorting and pagination would come next.
- "Today" uses the server time zone; hotels should have their own time zone.
- Frontend has no unit/e2e tests; no i18n; basic accessibility only (labels, focus styles, `role="alert"`).
- A scheduled job could prune old `room_inventory` rows for past dates.
