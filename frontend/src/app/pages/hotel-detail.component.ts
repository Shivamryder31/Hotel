import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Subscription, combineLatest } from 'rxjs';

import { ApiService } from '../api.service';
import { DateSuggestion, Hotel, Quote } from '../models';
import { addDaysIso, toApiError, todayIso, validateDates } from '../util';

@Component({
  selector: 'app-hotel-detail',
  standalone: true,
  imports: [FormsModule, RouterLink, CurrencyPipe, DatePipe],
  template: `
    <a routerLink="/" class="back">← All hotels</a>

    @if (loadError) {
      <p class="alert error" role="alert">{{ loadError }}</p>
    }

    @if (hotel; as h) {
      <div class="detail">
        <section class="panel">
          <h1>{{ h.name }}</h1>
          <p class="location">📍 {{ h.location }}</p>
          <p>{{ h.description }}</p>
          <ul class="chips">
            @for (a of h.amenities; track a) { <li>{{ a }}</li> }
          </ul>
          <p class="price-line">
            <span class="price">{{ h.pricePerNight | currency: 'INR' : 'symbol' : '1.0-0' }}</span>
            <span class="muted"> per night</span>
          </p>
        </section>

        <aside class="panel">
          <h2>Your stay</h2>
          <div class="dates">
            <label>Check-in
              <input type="date" [(ngModel)]="checkIn" [min]="today" (ngModelChange)="onCheckInChange()" />
            </label>
            <label>Check-out
              <input type="date" [(ngModel)]="checkOut" [min]="minCheckOut()" />
            </label>
          </div>
          <button class="btn" type="button" (click)="applyDates()">Check availability</button>
          @if (dateError) {
            <p class="alert error" role="alert">{{ dateError }}</p>
          }

          @if (quote; as q) {
            <div class="quote">
              <div class="row"><span>{{ q.pricePerNight | currency: 'INR' : 'symbol' : '1.0-0' }} × {{ q.nights }} {{ q.nights === 1 ? 'night' : 'nights' }}</span>
                <span>{{ q.totalAmount | currency: 'INR' : 'symbol' : '1.2-2' }}</span></div>
              <div class="row total"><span>Total</span><span>{{ q.totalAmount | currency: 'INR' : 'symbol' : '1.2-2' }}</span></div>
              @if (q.availableRooms === 0) {
                <span class="badge bad">Sold out for these dates</span>
              } @else if (q.availableRooms <= 2) {
                <span class="badge warn">Only {{ q.availableRooms }} left</span>
              } @else {
                <span class="badge good">{{ q.availableRooms }} rooms available</span>
              }
            </div>

            @if (q.availableRooms > 0) {
              <form class="book" (ngSubmit)="book()" #bf="ngForm" novalidate>
                <h3>Guest details</h3>
                <label>Full name
                  <input name="guestName" [(ngModel)]="guestName" maxlength="100" autocomplete="name"
                         [class.invalid]="fieldErrors['guestName']" />
                  @if (fieldErrors['guestName']) { <span class="field-error">{{ fieldErrors['guestName'] }}</span> }
                </label>
                <label>Email
                  <input name="guestEmail" type="email" [(ngModel)]="guestEmail" autocomplete="email"
                         [class.invalid]="fieldErrors['guestEmail']" />
                  @if (fieldErrors['guestEmail']) { <span class="field-error">{{ fieldErrors['guestEmail'] }}</span> }
                </label>
                @if (bookError) {
                  <p class="alert error" role="alert">{{ bookError }}</p>
                }
                <button class="btn primary block" type="submit" [disabled]="submitting">
                  {{ submitting ? 'Booking…' : 'Confirm booking · ' + (q.totalAmount | currency: 'INR' : 'symbol' : '1.2-2') }}
                </button>
                <p class="muted small">You can view or cancel this booking later using your reference and email.</p>
              </form>
            }
          } @else if (!dateError) {
            <p class="muted">Select dates to see the total price and available rooms.</p>
          }

          @if (quote && quote.availableRooms === 0) {
            <div class="suggest">
              <h3>✨ Flexible dates</h3>
              @if (alternatives.length) {
                <p class="muted">These {{ quote.nights }}-night stays are free at {{ hotel.name }}:</p>
                @for (s of alternatives; track s.checkIn) {
                  <button type="button" class="suggestion" (click)="useSuggestion(s)">
                    <span>
                      <strong>{{ s.checkIn | date: 'EEE d MMM' }} → {{ s.checkOut | date: 'EEE d MMM' }}</strong><br />
                      <small class="muted">{{ describeShift(s.shiftDays) }}</small>
                    </span>
                    <span>{{ s.totalAmount | currency: 'INR' : 'symbol' : '1.0-0' }}</span>
                  </button>
                }
              } @else {
                <p class="muted">No free {{ quote.nights }}-night stay within a week of these dates. Try other dates or another hotel.</p>
              }
            </div>
          }
        </aside>
      </div>
    }
  `,
})
export class HotelDetailComponent implements OnInit, OnDestroy {
  private readonly api = inject(ApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private sub?: Subscription;

  today = todayIso();
  hotel: Hotel | null = null;
  quote: Quote | null = null;
  alternatives: DateSuggestion[] = [];

  checkIn = '';
  checkOut = '';
  guestName = '';
  guestEmail = '';

  loadError = '';
  dateError = '';
  bookError = '';
  fieldErrors: Record<string, string> = {};
  submitting = false;

  ngOnInit(): void {
    this.sub = combineLatest([this.route.paramMap, this.route.queryParamMap]).subscribe(([params, query]) => {
      this.checkIn = query.get('checkIn') ?? '';
      this.checkOut = query.get('checkOut') ?? '';
      this.load(Number(params.get('id')));
    });
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
  }

  minCheckOut(): string {
    return this.checkIn ? addDaysIso(this.checkIn, 1) : this.today;
  }

  onCheckInChange(): void {
    if (this.checkIn && this.checkOut && this.checkOut <= this.checkIn) {
      this.checkOut = addDaysIso(this.checkIn, 1);
    }
  }

  applyDates(): void {
    this.dateError = validateDates(this.checkIn, this.checkOut) ?? '';
    if (this.dateError) return;
    this.router.navigate([], { queryParams: { checkIn: this.checkIn, checkOut: this.checkOut } });
  }

  useSuggestion(s: DateSuggestion): void {
    this.router.navigate([], { queryParams: { checkIn: s.checkIn, checkOut: s.checkOut } });
  }

  describeShift(days: number): string {
    const n = Math.abs(days);
    return `${n} ${n === 1 ? 'day' : 'days'} ${days < 0 ? 'earlier' : 'later'} than you searched`;
  }

  book(): void {
    if (!this.hotel || !this.quote || this.submitting) return;
    this.bookError = '';
    this.fieldErrors = {};

    // Light client-side checks for instant feedback; the backend validates everything again.
    if (!this.guestName.trim()) this.fieldErrors['guestName'] = 'Name is required.';
    if (!this.guestEmail.trim()) this.fieldErrors['guestEmail'] = 'Email is required.';
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(this.guestEmail.trim())) this.fieldErrors['guestEmail'] = 'Please enter a valid email address.';
    if (Object.keys(this.fieldErrors).length) return;

    this.submitting = true;
    this.api
      .createBooking({
        hotelId: this.hotel.id,
        checkIn: this.checkIn,
        checkOut: this.checkOut,
        guestName: this.guestName.trim(),
        guestEmail: this.guestEmail.trim(),
      })
      .subscribe({
        next: (booking) => {
          this.submitting = false;
          this.router.navigate(['/confirmation'], { state: { booking } });
        },
        error: (err) => {
          const e = toApiError(err);
          this.submitting = false;
          this.fieldErrors = e.fieldErrors ?? {};
          this.bookError = e.fieldErrors ? '' : e.message;
          // The room may have just been taken by someone else: refresh availability and suggestions.
          if (e.code === 'ROOM_UNAVAILABLE' && this.hotel) this.load(this.hotel.id, true);
        },
      });
  }

  private load(id: number, keepBookError = false): void {
    this.loadError = '';
    this.dateError = '';
    if (!keepBookError) this.bookError = '';
    this.quote = null;
    this.alternatives = [];

    const hasDates = !!(this.checkIn && this.checkOut);
    const invalid = hasDates ? validateDates(this.checkIn, this.checkOut) : null;
    if (invalid) this.dateError = invalid;
    const useDates = hasDates && !invalid;

    this.api.getHotel(id).subscribe({
      next: (hotel) => (this.hotel = hotel),
      error: (err) => (this.loadError = toApiError(err).message),
    });
    if (!useDates) return;

    this.api.getQuote(id, this.checkIn, this.checkOut).subscribe({
      next: (quote) => {
        this.quote = quote;
        if (quote.availableRooms === 0) {
          this.api.getAlternatives(id, this.checkIn, this.checkOut).subscribe({
            next: (list) => (this.alternatives = list),
            error: () => (this.alternatives = []),
          });
        }
      },
      error: (err) => (this.dateError = toApiError(err).message),
    });
  }
}
