import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { MessageResponse, RolePermission } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class RolePermissionApiService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/role-permission`;

  byRole(roleId: string): Observable<RolePermission[]> {
    return this.http.get<RolePermission[]>(`${this.base}/role/${roleId}`);
  }

  assign(roleId: string, permissionId: string): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(`${this.base}/role/${roleId}/permission/${permissionId}`, {});
  }

  remove(rolePermissionId: string): Observable<MessageResponse> {
    return this.http.delete<MessageResponse>(`${this.base}/${rolePermissionId}`);
  }
}
