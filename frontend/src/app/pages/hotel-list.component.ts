import { CurrencyPipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { ApiService } from '../api.service';
import { Hotel } from '../models';
import { addDaysIso, toApiError, todayIso, validateDates } from '../util';

@Component({
  selector: 'app-hotel-list',
  standalone: true,
  imports: [FormsModule, RouterLink, CurrencyPipe],
  template: `
    <section class="hero">
      <h1>Find your next stay</h1>
      <p>Search by city and pick your dates to see live availability.</p>
      <form class="search" (ngSubmit)="search()" #f="ngForm">
        <label>Location
          <input name="location" [(ngModel)]="location" placeholder="e.g. Goa, Jaipur" />
        </label>
        <label>Check-in
          <input type="date" name="checkIn" [(ngModel)]="checkIn" [min]="today" (ngModelChange)="onCheckInChange()" />
        </label>
        <label>Check-out
          <input type="date" name="checkOut" [(ngModel)]="checkOut" [min]="minCheckOut()" />
        </label>
        <button type="submit" class="btn primary" [disabled]="loading">Search</button>
      </form>
      @if (dateError) {
        <p class="alert error" role="alert">{{ dateError }}</p>
      }
    </section>

    @if (error) {
      <p class="alert error" role="alert">{{ error }}</p>
    }
    @if (loading) {
      <p class="muted">Loading hotels…</p>
    }

    @if (!loading && !error) {
      <p class="muted results-count">
        {{ hotels.length }} {{ hotels.length === 1 ? 'hotel' : 'hotels' }}
        @if (searchedDates) { for {{ searchedDates.checkIn }} → {{ searchedDates.checkOut }} }
      </p>
      @if (hotels.length === 0) {
        <div class="empty">No hotels match that location. Try a different city.</div>
      }
      <div class="grid">
        @for (h of hotels; track h.id) {
          <article class="card" [class.soldout]="h.availableRooms === 0">
            <div class="card-head">
              <h2>{{ h.name }}</h2>
              <span class="location">📍 {{ h.location }}</span>
            </div>
            <p class="desc">{{ h.description }}</p>
            <ul class="chips">
              @for (a of h.amenities; track a) { <li>{{ a }}</li> }
            </ul>
            <div class="card-foot">
              <div>
                <span class="price">{{ h.pricePerNight | currency: 'INR' : 'symbol' : '1.0-0' }}</span>
                <span class="muted"> / night</span>
              </div>
              @if (h.availableRooms !== null) {
                @if (h.availableRooms === 0) {
                  <span class="badge bad">Sold out</span>
                } @else if (h.availableRooms <= 2) {
                  <span class="badge warn">Only {{ h.availableRooms }} left</span>
                } @else {
                  <span class="badge good">{{ h.availableRooms }} rooms available</span>
                }
              }
            </div>
            <a class="btn" [class.primary]="h.availableRooms !== 0" [routerLink]="['/hotels', h.id]" [queryParams]="detailParams()">
              {{ h.availableRooms === 0 ? 'See other dates' : 'View & book' }}
            </a>
          </article>
        }
      </div>
    }
  `,
})
export class HotelListComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  today = todayIso();
  location = '';
  checkIn = '';
  checkOut = '';
  hotels: Hotel[] = [];
  loading = false;
  error = '';
  dateError = '';
  searchedDates: { checkIn: string; checkOut: string } | null = null;

  ngOnInit(): void {
    const q = this.route.snapshot.queryParamMap;
    this.location = q.get('location') ?? '';
    this.checkIn = q.get('checkIn') ?? '';
    this.checkOut = q.get('checkOut') ?? '';
    if (this.checkIn && this.checkOut && validateDates(this.checkIn, this.checkOut)) {
      this.checkIn = '';
      this.checkOut = '';
    }
    this.load();
  }

  minCheckOut(): string {
    return this.checkIn ? addDaysIso(this.checkIn, 1) : this.today;
  }

  onCheckInChange(): void {
    if (this.checkIn && this.checkOut && this.checkOut <= this.checkIn) {
      this.checkOut = addDaysIso(this.checkIn, 1);
    }
  }

  search(): void {
    this.dateError = '';
    const hasAnyDate = !!(this.checkIn || this.checkOut);
    if (hasAnyDate) {
      const problem = validateDates(this.checkIn, this.checkOut);
      if (problem) {
        this.dateError = problem;
        return;
      }
    }
    this.router.navigate([], {
      queryParams: { location: this.location || null, checkIn: this.checkIn || null, checkOut: this.checkOut || null },
      replaceUrl: true,
    });
    this.load();
  }

  detailParams(): Record<string, string> {
    return this.searchedDates ? { ...this.searchedDates } : {};
  }

  private load(): void {
    this.loading = true;
    this.error = '';
    const withDates = this.checkIn && this.checkOut;
    this.api.searchHotels(this.location, withDates ? this.checkIn : undefined, withDates ? this.checkOut : undefined).subscribe({
      next: (hotels) => {
        this.hotels = hotels;
        this.searchedDates = withDates ? { checkIn: this.checkIn, checkOut: this.checkOut } : null;
        this.loading = false;
      },
      error: (err) => {
        this.error = toApiError(err).message;
        this.loading = false;
      },
    });
  }
}
