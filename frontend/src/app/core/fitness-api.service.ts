import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  AuthResponse,
  RecoveryAssessment,
  RecoveryCheckin,
  RecoveryOverview
} from './recovery.models';

@Injectable({ providedIn: 'root' })
export class FitnessApiService {
  private readonly apiUrl = 'http://localhost:8080/api';
  private readonly tokenKey = 'fitcoach_token';

  constructor(private readonly http: HttpClient) {}

  generatePlan(): Observable<unknown> {
    return this.http.post(`${this.apiUrl}/plans/generate`, {}, { headers: this.authHeaders() });
  }

  generateGuestPlan(request: Record<string, unknown>): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/public/plans/generate`, request);
  }

  connectStrava(): Observable<{ url: string }> {
    return this.http.get<{ url: string }>(`${this.apiUrl}/strava/connect`);
  }

  syncStrava(): Observable<string> {
    return this.http.post(`${this.apiUrl}/strava/sync`, {}, {
      headers: this.authHeaders(),
      responseType: 'text'
    });
  }

  getRecoveryOverview(): Observable<RecoveryOverview> {
    return this.http.get<RecoveryOverview>(`${this.apiUrl}/recovery/overview`, { headers: this.authHeaders() });
  }

  generateRecoveryAssessment(checkin?: Partial<RecoveryCheckin> | null): Observable<RecoveryAssessment> {
    const body = checkin ? { checkin } : {};
    return this.http.post<RecoveryAssessment>(`${this.apiUrl}/recovery/assessments`, body, {
      headers: this.authHeaders()
    });
  }

  getRecoveryHistory(): Observable<RecoveryAssessment[]> {
    return this.http.get<RecoveryAssessment[]>(`${this.apiUrl}/recovery/assessments`, {
      headers: this.authHeaders()
    });
  }

  saveRecoveryCheckin(checkin: Partial<RecoveryCheckin>): Observable<RecoveryCheckin> {
    return this.http.post<RecoveryCheckin>(`${this.apiUrl}/recovery/checkins`, checkin, {
      headers: this.authHeaders()
    });
  }

  login(email: string, password: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/auth/login`, { email, password });
  }

  register(payload: { email: string; password: string; firstName: string; lastName: string }): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/auth/register`, payload);
  }

  hasToken(): boolean {
    return !!localStorage.getItem(this.tokenKey);
  }

  storeToken(token: string): void {
    localStorage.setItem(this.tokenKey, token);
  }

  private authHeaders(): HttpHeaders {
    const token = localStorage.getItem(this.tokenKey);
    return token ? new HttpHeaders({ Authorization: `Bearer ${token}` }) : new HttpHeaders();
  }
}
