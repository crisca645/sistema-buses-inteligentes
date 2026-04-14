import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { MessageResponse } from '../models/api.models';
import { Role } from '../models/role.model';

@Injectable({ providedIn: 'root' })
export class RolesApiService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/roles`;

  list(): Observable<Role[]> {
    return this.http.get<Role[]>(`${this.base}`);
  }

  create(role: Role): Observable<Role> {
    return this.http.post<Role>(`${this.base}`, role);
  }

  update(id: string, role: Role): Observable<Role> {
    return this.http.put<Role>(`${this.base}/${id}`, role);
  }

  delete(id: string): Observable<MessageResponse> {
    return this.http.delete<MessageResponse>(`${this.base}/${id}`);
  }
}
