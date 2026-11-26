import { Routes } from '@angular/router';

import { ConfirmationComponent } from './pages/confirmation.component';
import { HotelDetailComponent } from './pages/hotel-detail.component';
import { HotelListComponent } from './pages/hotel-list.component';
import { ManageBookingComponent } from './pages/manage-booking.component';

export const routes: Routes = [
  { path: '', component: HotelListComponent, title: 'Find a hotel' },
  { path: 'hotels/:id', component: HotelDetailComponent, title: 'Hotel details' },
  { path: 'confirmation', component: ConfirmationComponent, title: 'Booking confirmed' },
  { path: 'manage', component: ManageBookingComponent, title: 'Manage booking' },
  { path: '**', redirectTo: '' },
];
