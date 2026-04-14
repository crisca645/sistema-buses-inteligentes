import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { jwtDecode } from 'jwt-decode';
import { JwtPayload } from '../models/jwt-payload.model';
import { User } from '../models/api.models';

const TOKEN_KEY = 'ms_security_jwt';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly router = inject(Router);

  private readonly tokenSignal = signal<string | null>(this.readStoredToken());

  readonly token = this.tokenSignal.asReadonly();

  readonly isAuthenticated = computed(() => {
    const t = this.tokenSignal();
    if (!t) return false;
    const payload = this.decodePayload(t);
    if (!payload?.exp) return true;
    return payload.exp * 1000 > Date.now();
  });

  readonly payload = computed(() => {
    const t = this.tokenSignal();
    return t ? this.decodePayload(t) : null;
  });

  readonly roles = computed(() => {
    const r = this.payload()?.role;
    if (!r) return [] as string[];
    return r.split(',').map((x) => x.trim()).filter(Boolean);
  });

  setToken(token: string | null): void {
    if (token) {
      localStorage.setItem(TOKEN_KEY, token);
      this.tokenSignal.set(token);
    } else {
      localStorage.removeItem(TOKEN_KEY);
      this.tokenSignal.set(null);
    }
  }

  logout(redirect = true): void {
    this.setToken(null);
    if (redirect) {
      void this.router.navigate(['/login']);
    }
  }

  decodePayload(token: string): JwtPayload | null {
    try {
      return jwtDecode<JwtPayload>(token);
    } catch {
      return null;
    }
  }

  currentUserSnapshot(): User | null {
    const p = this.payload();
    if (!p?.id) return null;
    return {
      id: p.id,
      name: p.name,
      email: p.email,
      username: p.username,
    };
  }

  private readStoredToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }
}
