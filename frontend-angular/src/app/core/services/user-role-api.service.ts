import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  MessageResponse,
  UserAvailableRolesResponse,
  UserRoleByUserResponse,
  UserWithRolesResponse,
} from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class UserRoleApiService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/user-role`;

  usersWithRoles(search?: string): Observable<UserWithRolesResponse[]> {
    let params = new HttpParams();
    if (search?.trim()) {
      params = params.set('search', search.trim());
    }
    return this.http.get<UserWithRolesResponse[]>(`${this.base}/users-with-roles`, { params });
  }

  userRoles(userId: string): Observable<UserRoleByUserResponse[]> {
    return this.http.get<UserRoleByUserResponse[]>(`${this.base}/user/${userId}/roles`);
  }

  availableRoles(userId: string): Observable<UserAvailableRolesResponse> {
    return this.http.get<UserAvailableRolesResponse>(`${this.base}/user/${userId}/available-roles`);
  }

  assign(userId: string, roleId: string): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(`${this.base}/user/${userId}/role/${roleId}`, {});
  }

  remove(userRoleId: string): Observable<MessageResponse> {
    return this.http.delete<MessageResponse>(`${this.base}/${userRoleId}`);
  }
}
