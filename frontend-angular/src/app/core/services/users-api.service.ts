import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { MessageResponse, RegisterRequest, User } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class UsersApiService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/users`;

  list(): Observable<User[]> {
    return this.http.get<User[]>(`${this.base}`);
  }

  register(body: RegisterRequest): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(`${this.base}/register`, body);
  }
}
