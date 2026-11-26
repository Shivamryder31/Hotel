package com.example.hotelbooking.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.hotelbooking.domain.Booking;
import com.example.hotelbooking.domain.BookingStatus;
import com.example.hotelbooking.domain.Hotel;
import com.example.hotelbooking.domain.RoomInventory;
import com.example.hotelbooking.exception.ApiException;
import com.example.hotelbooking.repository.BookingRepository;
import com.example.hotelbooking.repository.HotelRepository;
import com.example.hotelbooking.repository.RoomInventoryRepository;
import com.example.hotelbooking.web.dto.CreateBookingRequest;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 10);
    private static final LocalDate CHECK_IN = TODAY.plusDays(10);

    @Mock HotelRepository hotelRepository;
    @Mock RoomInventoryRepository inventoryRepository;
    @Mock BookingRepository bookingRepository;

    BookingService service;
    Hotel hotel;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(TODAY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        service = new BookingService(hotelRepository, inventoryRepository, bookingRepository, clock);
        hotel = new Hotel(1L, "Test Hotel", "Goa", "desc", List.of("Pool"), new BigDecimal("6500.00"), 2);

        lenient().when(hotelRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(hotel));
        lenient().when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private CreateBookingRequest request(LocalDate in, LocalDate out) {
        return new CreateBookingRequest(1L, in, out, "  Asha Rao ", " Asha@Example.com ");
    }

    private RoomInventory inventoryRow(LocalDate night, int booked) {
        RoomInventory row = new RoomInventory(1L, night);
        for (int i = 0; i < booked; i++) {
            row.reserve();
        }
        return row;
    }

    @Test
    void bookingComputesTotalOnTheBackendAndNormalisesGuestDetails() {
        when(inventoryRepository.findForHotel(eq(1L), any(), any())).thenReturn(List.of());

        Booking booking = service.createBooking(request(CHECK_IN, CHECK_IN.plusDays(3)));

        assertEquals(3, booking.getNights());
        assertEquals(new BigDecimal("19500.00"), booking.getTotalAmount());
        assertEquals("Asha Rao", booking.getGuestName());
        assertEquals("asha@example.com", booking.getGuestEmail());
        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
        assertTrue(booking.getReference().startsWith("HB-"));
    }

    @Test
    void bookingReservesOneRoomOnEveryNightButNotTheCheckOutDay() {
        when(inventoryRepository.findForHotel(eq(1L), any(), any()))
                .thenReturn(List.of(inventoryRow(CHECK_IN, 1)));

        service.createBooking(request(CHECK_IN, CHECK_IN.plusDays(2)));

        @SuppressWarnings("unchecked")
        org.mockito.ArgumentCaptor<Iterable<RoomInventory>> captor = org.mockito.ArgumentCaptor.forClass(Iterable.class);
        verify(inventoryRepository).saveAll(captor.capture());
        List<RoomInventory> saved = new ArrayList<>();
        captor.getValue().forEach(saved::add);

        assertEquals(2, saved.size());
        for (RoomInventory row : saved) {
            assertTrue(row.getStayDate().isBefore(CHECK_IN.plusDays(2)), "check-out day must not be reserved");
            if (row.getStayDate().equals(CHECK_IN)) {
                assertEquals(2, row.getBookedRooms());   // 1 existing + this booking
            } else {
                assertEquals(1, row.getBookedRooms());
            }
        }
    }

    @Test
    void bookingIsRejectedWhenAnyNightIsFull() {
        // capacity is 2; the middle night is already fully booked by overlapping stays
        when(inventoryRepository.findForHotel(eq(1L), any(), any()))
                .thenReturn(List.of(inventoryRow(CHECK_IN.plusDays(1), 2)));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.createBooking(request(CHECK_IN, CHECK_IN.plusDays(3))));

        assertEquals(ApiException.ROOM_UNAVAILABLE, ex.getCode());
        verify(inventoryRepository, never()).saveAll(any());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void lastRoomCanBeBookedOnceButNotTwice() {
        // capacity 2, one room already taken: first request gets the last room...
        RoomInventory night = inventoryRow(CHECK_IN, 1);
        when(inventoryRepository.findForHotel(eq(1L), any(), any())).thenReturn(List.of(night));

        service.createBooking(request(CHECK_IN, CHECK_IN.plusDays(1)));
        assertEquals(2, night.getBookedRooms());

        // ...and the second request (which would run after the first released the hotel lock) is refused.
        ApiException ex = assertThrows(ApiException.class,
                () -> service.createBooking(request(CHECK_IN, CHECK_IN.plusDays(1))));
        assertEquals(ApiException.ROOM_UNAVAILABLE, ex.getCode());
    }

    @Test
    void invalidDatesNeverReachTheDatabase() {
        assertThrows(ApiException.class, () -> service.createBooking(request(TODAY.minusDays(1), TODAY.plusDays(1))));
        assertThrows(ApiException.class, () -> service.createBooking(request(CHECK_IN, CHECK_IN)));
        verify(hotelRepository, never()).findByIdForUpdate(anyLong());
    }

    @Test
    void cancellingReleasesTheReservedNights() {
        Booking booking = new Booking("HB-ABCD2345", hotel, "Asha", "asha@example.com",
                CHECK_IN, CHECK_IN.plusDays(2), 2, hotel.getPricePerNight(), new BigDecimal("13000.00"), Instant.now());
        RoomInventory n1 = inventoryRow(CHECK_IN, 2);
        RoomInventory n2 = inventoryRow(CHECK_IN.plusDays(1), 1);
        when(bookingRepository.findForUpdate("HB-ABCD2345", "asha@example.com")).thenReturn(Optional.of(booking));
        when(inventoryRepository.findForHotel(eq(1L), any(), any())).thenReturn(List.of(n1, n2));

        Booking result = service.cancelBooking(" hb-abcd2345 ", "ASHA@example.com");

        assertEquals(BookingStatus.CANCELLED, result.getStatus());
        assertEquals(1, n1.getBookedRooms());
        assertEquals(0, n2.getBookedRooms());
    }

    @Test
    void cancellingTwiceIsRejectedAndDoesNotReleaseAgain() {
        Booking booking = new Booking("HB-ABCD2345", hotel, "Asha", "asha@example.com",
                CHECK_IN, CHECK_IN.plusDays(1), 1, hotel.getPricePerNight(), new BigDecimal("6500.00"), Instant.now());
        booking.cancel(Instant.now());
        when(bookingRepository.findForUpdate(anyString(), anyString())).thenReturn(Optional.of(booking));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.cancelBooking("HB-ABCD2345", "asha@example.com"));

        assertEquals(ApiException.ALREADY_CANCELLED, ex.getCode());
        verify(inventoryRepository, never()).saveAll(any());
    }

    @Test
    void lookupWithWrongEmailLooksLikeUnknownReference() {
        when(bookingRepository.findByReferenceAndGuestEmail(anyString(), anyString())).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class,
                () -> service.findBooking("HB-ABCD2345", "someone@else.com"));

        assertEquals(ApiException.NOT_FOUND, ex.getCode());
    }
}
