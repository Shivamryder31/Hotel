import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <header class="topbar">
      <div class="container topbar-inner">
        <a routerLink="/" class="brand">🏨 StayEasy</a>
        <nav>
          <a routerLink="/" routerLinkActive="active" [routerLinkActiveOptions]="{ exact: true }">Hotels</a>
          <a routerLink="/manage" routerLinkActive="active">Manage booking</a>
        </nav>
      </div>
    </header>
    <main class="container">
      <router-outlet />
    </main>
  `,
})
export class AppComponent {}
