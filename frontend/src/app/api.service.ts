import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Booking, CreateBookingRequest, DateSuggestion, Hotel, Quote } from './models';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api';

  searchHotels(location: string, checkIn?: string, checkOut?: string): Observable<Hotel[]> {
    let params = new HttpParams();
    if (location.trim()) params = params.set('location', location.trim());
    if (checkIn && checkOut) params = params.set('checkIn', checkIn).set('checkOut', checkOut);
    return this.http.get<Hotel[]>(`${this.base}/hotels`, { params });
  }

  getHotel(id: number, checkIn?: string, checkOut?: string): Observable<Hotel> {
    let params = new HttpParams();
    if (checkIn && checkOut) params = params.set('checkIn', checkIn).set('checkOut', checkOut);
    return this.http.get<Hotel>(`${this.base}/hotels/${id}`, { params });
  }

  getQuote(id: number, checkIn: string, checkOut: string): Observable<Quote> {
    const params = new HttpParams().set('checkIn', checkIn).set('checkOut', checkOut);
    return this.http.get<Quote>(`${this.base}/hotels/${id}/quote`, { params });
  }

  getAlternatives(id: number, checkIn: string, checkOut: string): Observable<DateSuggestion[]> {
    const params = new HttpParams().set('checkIn', checkIn).set('checkOut', checkOut);
    return this.http.get<DateSuggestion[]>(`${this.base}/hotels/${id}/alternatives`, { params });
  }

  createBooking(request: CreateBookingRequest): Observable<Booking> {
    return this.http.post<Booking>(`${this.base}/bookings`, request);
  }

  findBooking(reference: string, email: string): Observable<Booking> {
    const params = new HttpParams().set('email', email.trim());
    return this.http.get<Booking>(`${this.base}/bookings/${encodeURIComponent(reference.trim())}`, { params });
  }

  cancelBooking(reference: string, email: string): Observable<Booking> {
    return this.http.post<Booking>(`${this.base}/bookings/${encodeURIComponent(reference.trim())}/cancel`, {
      email: email.trim(),
    });
  }
}
