import { HttpErrorResponse } from '@angular/common/http';

import { ApiError } from './models';

function pad(n: number): string {
  return n < 10 ? `0${n}` : `${n}`;
}

/** yyyy-MM-dd in the browser's local time zone (toISOString would shift to UTC). */
export function toIso(d: Date): string {
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

export function todayIso(): string {
  return toIso(new Date());
}

export function addDaysIso(iso: string, days: number): string {
  const [y, m, d] = iso.split('-').map(Number);
  return toIso(new Date(y, m - 1, d + days));
}

/** Mirrors the backend rules so users get instant feedback; the backend still enforces them. */
export function validateDates(checkIn: string, checkOut: string): string | null {
  if (!checkIn || !checkOut) return 'Please select both a check-in and a check-out date.';
  if (checkIn < todayIso()) return 'Check-in date cannot be in the past.';
  if (checkOut <= checkIn) return 'Check-out date must be after the check-in date.';
  return null;
}

export function toApiError(err: unknown): ApiError {
  if (err instanceof HttpErrorResponse) {
    const body = err.error as Partial<ApiError> | null;
    if (body && typeof body === 'object' && body.message) {
      return { status: err.status, code: body.code ?? 'ERROR', message: body.message, fieldErrors: body.fieldErrors };
    }
    if (err.status === 0) {
      return { status: 0, code: 'NETWORK', message: 'Cannot reach the server. Please check your connection and try again.' };
    }
  }
  return { status: 500, code: 'ERROR', message: 'Something went wrong. Please try again.' };
}
