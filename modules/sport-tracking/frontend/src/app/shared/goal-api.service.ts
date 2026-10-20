import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Goal, GoalRequest } from './models';

@Injectable({ providedIn: 'root' })
export class GoalApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/api/users`;

  list(userId: number): Observable<Goal[]> {
    return this.http.get<Goal[]>(`${this.baseUrl}/${userId}/goals`);
  }

  create(userId: number, request: GoalRequest): Observable<Goal> {
    return this.http.post<Goal>(`${this.baseUrl}/${userId}/goals`, request);
  }

  update(userId: number, goalId: number, request: GoalRequest): Observable<Goal> {
    return this.http.put<Goal>(`${this.baseUrl}/${userId}/goals/${goalId}`, request);
  }

  delete(userId: number, goalId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${userId}/goals/${goalId}`);
  }
}
