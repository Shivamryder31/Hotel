import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';

import { Booking } from '../models';

@Component({
  selector: 'app-confirmation',
  standalone: true,
  imports: [RouterLink, CurrencyPipe, DatePipe],
  template: `
    @if (booking; as b) {
      <section class="panel confirm">
        <div class="tick">✓</div>
        <h1>Booking confirmed</h1>
        <p class="muted">Keep your reference handy — you'll need it with your email to view or cancel.</p>
        <div class="reference" aria-label="Booking reference">{{ b.reference }}</div>

        <dl class="summary">
          <dt>Hotel</dt><dd>{{ b.hotelName }}, {{ b.hotelLocation }}</dd>
          <dt>Guest</dt><dd>{{ b.guestName }} · {{ b.guestEmail }}</dd>
          <dt>Check-in</dt><dd>{{ b.checkIn | date: 'EEE, d MMM y' }}</dd>
          <dt>Check-out</dt><dd>{{ b.checkOut | date: 'EEE, d MMM y' }}</dd>
          <dt>Nights</dt><dd>{{ b.nights }} × {{ b.pricePerNight | currency: 'INR' : 'symbol' : '1.0-0' }}</dd>
          <dt>Total</dt><dd><strong>{{ b.totalAmount | currency: 'INR' : 'symbol' : '1.2-2' }}</strong></dd>
        </dl>

        <div class="actions">
          <a class="btn primary" routerLink="/manage" [queryParams]="{ reference: b.reference }">Manage booking</a>
          <a class="btn" routerLink="/">Book another stay</a>
        </div>
      </section>
    }
  `,
})
export class ConfirmationComponent implements OnInit {
  private readonly router = inject(Router);
  booking: Booking | null = null;

  ngOnInit(): void {
    // The booking arrives via router state; on a refresh it is gone, so send the user to look it up.
    this.booking = (history.state?.booking as Booking | undefined) ?? null;
    if (!this.booking) this.router.navigate(['/manage']);
  }
}
