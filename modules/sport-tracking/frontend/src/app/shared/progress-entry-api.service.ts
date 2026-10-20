import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { ProgressEntry, ProgressEntryRequest } from './models';

@Injectable({ providedIn: 'root' })
export class ProgressEntryApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/api/users`;

  summarize(userId: number): Observable<{summary: string; model: string; measurementCount: number}> {
    return this.http.post<{summary: string; model: string; measurementCount: number}>(
      `${this.baseUrl}/${userId}/progress-summary`, {});
  }

  list(userId: number): Observable<ProgressEntry[]> {
    return this.http.get<ProgressEntry[]>(`${this.baseUrl}/${userId}/progress-entries`);
  }

  create(userId: number, request: ProgressEntryRequest): Observable<ProgressEntry> {
    return this.http.post<ProgressEntry>(`${this.baseUrl}/${userId}/progress-entries`, request);
  }

  update(userId: number, entryId: number, request: ProgressEntryRequest): Observable<ProgressEntry> {
    return this.http.put<ProgressEntry>(`${this.baseUrl}/${userId}/progress-entries/${entryId}`, request);
  }

  delete(userId: number, entryId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${userId}/progress-entries/${entryId}`);
  }
}
