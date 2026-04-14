import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Permission } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class PermissionsApiService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/permissions`;

  list(): Observable<Permission[]> {
    return this.http.get<Permission[]>(`${this.base}`);
  }
}
