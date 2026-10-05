import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class FitnessApiService {
  private readonly apiUrl = 'http://localhost:8080/api';

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

  private authHeaders(): HttpHeaders {
    const token = localStorage.getItem('fitcoach_token');
    return token ? new HttpHeaders({ Authorization: `Bearer ${token}` }) : new HttpHeaders();
  }
}
