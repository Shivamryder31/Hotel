import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { ApiService } from '../api.service';
import { Booking } from '../models';
import { toApiError, todayIso } from '../util';

@Component({
  selector: 'app-manage-booking',
  standalone: true,
  imports: [FormsModule, RouterLink, CurrencyPipe, DatePipe],
  template: `
    <section class="panel narrow">
      <h1>Manage your booking</h1>
      <form (ngSubmit)="lookup()" novalidate>
        <label>Booking reference
          <input name="reference" [(ngModel)]="reference" placeholder="HB-XXXXXXXX" autocapitalize="characters" />
        </label>
        <label>Email used for the booking
          <input name="email" type="email" [(ngModel)]="email" autocomplete="email" />
        </label>
        <button class="btn primary" type="submit" [disabled]="busy">Find booking</button>
      </form>
      @if (error) {
        <p class="alert error" role="alert">{{ error }}</p>
      }
    </section>

    @if (booking; as b) {
      <section class="panel narrow">
        <div class="status-row">
          <h2>{{ b.hotelName }}</h2>
          <span class="badge" [class.good]="b.status === 'CONFIRMED'" [class.bad]="b.status === 'CANCELLED'">
            {{ b.status === 'CONFIRMED' ? 'Confirmed' : 'Cancelled' }}
          </span>
        </div>
        <p class="muted">{{ b.hotelLocation }} · Ref <strong>{{ b.reference }}</strong></p>
        <dl class="summary">
          <dt>Guest</dt><dd>{{ b.guestName }}</dd>
          <dt>Check-in</dt><dd>{{ b.checkIn | date: 'EEE, d MMM y' }}</dd>
          <dt>Check-out</dt><dd>{{ b.checkOut | date: 'EEE, d MMM y' }}</dd>
          <dt>Nights</dt><dd>{{ b.nights }} × {{ b.pricePerNight | currency: 'INR' : 'symbol' : '1.0-0' }}</dd>
          <dt>Total</dt><dd><strong>{{ b.totalAmount | currency: 'INR' : 'symbol' : '1.2-2' }}</strong></dd>
        </dl>

        @if (b.status === 'CANCELLED') {
          <p class="alert info">This booking was cancelled and the room has been released.</p>
          <a class="btn" routerLink="/">Find another stay</a>
        } @else if (b.checkIn < today) {
          <p class="muted">This stay has already started, so it can no longer be cancelled online.</p>
        } @else {
          <button class="btn danger" type="button" (click)="cancel()" [disabled]="busy">Cancel this booking</button>
        }
      </section>
    }
  `,
})
export class ManageBookingComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly route = inject(ActivatedRoute);

  today = todayIso();
  reference = '';
  email = '';
  booking: Booking | null = null;
  error = '';
  busy = false;

  ngOnInit(): void {
    this.reference = this.route.snapshot.queryParamMap.get('reference') ?? '';
  }

  lookup(): void {
    this.error = '';
    this.booking = null;
    if (!this.reference.trim() || !this.email.trim()) {
      this.error = 'Please enter both your booking reference and email address.';
      return;
    }
    this.busy = true;
    this.api.findBooking(this.reference, this.email).subscribe({
      next: (b) => {
        this.booking = b;
        this.busy = false;
      },
      error: (err) => {
        this.error = toApiError(err).message;
        this.busy = false;
      },
    });
  }

  cancel(): void {
    if (!this.booking || !confirm(`Cancel your stay at ${this.booking.hotelName}? This cannot be undone.`)) return;
    this.error = '';
    this.busy = true;
    this.api.cancelBooking(this.booking.reference, this.email).subscribe({
      next: (b) => {
        this.booking = b;
        this.busy = false;
      },
      error: (err) => {
        this.error = toApiError(err).message;
        this.busy = false;
      },
    });
  }
}
