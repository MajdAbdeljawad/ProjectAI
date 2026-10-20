import { Injectable, signal } from '@angular/core';

const USER_ID_KEY = 'sport-tracking-user-id';

@Injectable({ providedIn: 'root' })
export class UserContextService {
  readonly userId = signal(this.readUserId());

  setUserId(value: number | string): void {
    const userId = Number(value);
    if (!Number.isSafeInteger(userId) || userId < 1) return;
    this.userId.set(userId);
    if (typeof localStorage !== 'undefined') localStorage.setItem(USER_ID_KEY, String(userId));
  }

  private readUserId(): number {
    if (typeof localStorage === 'undefined') return 1;
    const stored = Number(localStorage.getItem(USER_ID_KEY));
    return Number.isSafeInteger(stored) && stored > 0 ? stored : 1;
  }
}
