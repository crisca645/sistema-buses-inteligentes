import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';

function shouldAttachAuth(url: string): boolean {
  const u = url.toLowerCase();
  if (u.includes('/security/login')) return false;
  if (u.includes('/security/forgot-password')) return false;
  if (u.includes('/security/reset-password')) return false;
  if (u.includes('/security/2fa/')) return false;
  if (u.includes('/security/github/register-alternate-email')) return false;
  if (u.includes('/users/register')) return false;
  if (u.includes('/oauth2/')) return false;
  if (u.includes('/login/oauth2/')) return false;
  return true;
}

export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const token = auth.token();
  if (!token || !shouldAttachAuth(req.url)) {
    return next(req);
  }
  const clone = req.clone({
    setHeaders: { Authorization: `Bearer ${token}` },
  });
  return next(clone);
};
