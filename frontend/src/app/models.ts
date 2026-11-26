export interface Hotel {
  id: number;
  name: string;
  location: string;
  description: string;
  amenities: string[];
  pricePerNight: number;
  /** null when no dates were supplied */
  availableRooms: number | null;
}

export interface Quote {
  hotelId: number;
  hotelName: string;
  checkIn: string;
  checkOut: string;
  nights: number;
  pricePerNight: number;
  totalAmount: number;
  availableRooms: number;
}

export interface DateSuggestion {
  checkIn: string;
  checkOut: string;
  nights: number;
  shiftDays: number;
  availableRooms: number;
  totalAmount: number;
}

export interface Booking {
  reference: string;
  status: 'CONFIRMED' | 'CANCELLED';
  hotelId: number;
  hotelName: string;
  hotelLocation: string;
  guestName: string;
  guestEmail: string;
  checkIn: string;
  checkOut: string;
  nights: number;
  pricePerNight: number;
  totalAmount: number;
  createdAt: string;
  cancelledAt: string | null;
}

export interface CreateBookingRequest {
  hotelId: number;
  checkIn: string;
  checkOut: string;
  guestName: string;
  guestEmail: string;
}

/** Shape of every error body returned by the backend. */
export interface ApiError {
  status: number;
  code: string;
  message: string;
  fieldErrors?: Record<string, string>;
}
