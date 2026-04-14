import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AuthResponse,
  CompleteProfileResponse,
  GithubAlternateEmailRequest,
  Login2FAResponse,
  LoginRequest,
  MessageResponse,
  TwoFactorResponse,
} from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class SecurityApiService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/security`;

  login(body: LoginRequest): Observable<Login2FAResponse & Record<string, unknown>> {
    return this.http.post<Login2FAResponse & Record<string, unknown>>(`${this.base}/login`, body);
  }

  verify2FA(body: { sessionId: string; code: string }): Observable<TwoFactorResponse> {
    return this.http.post<TwoFactorResponse>(`${this.base}/2fa/verify`, body);
  }

  resend2FA(body: { sessionId: string }): Observable<TwoFactorResponse> {
    return this.http.post<TwoFactorResponse>(`${this.base}/2fa/resend`, body);
  }

  cancel2FA(body: { sessionId: string }): Observable<TwoFactorResponse> {
    return this.http.post<TwoFactorResponse>(`${this.base}/2fa/cancel`, body);
  }

  forgotPassword(body: { email: string; recaptchaToken: string }): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(`${this.base}/forgot-password`, body);
  }

  resetPassword(body: {
    token: string;
    newPassword: string;
    confirmPassword: string;
  }): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(`${this.base}/reset-password`, body);
  }

  completeProfile(body: { userId: string; address: string; phone: string }): Observable<CompleteProfileResponse> {
    return this.http.put<CompleteProfileResponse>(`${this.base}/complete-profile`, body);
  }

  unlinkGoogle(): Observable<MessageResponse> {
    return this.http.put<MessageResponse>(`${this.base}/unlink/google`, {});
  }

  unlinkGithub(): Observable<MessageResponse> {
    return this.http.put<MessageResponse>(`${this.base}/unlink/github`, {});
  }

  completeGithubEmail(email: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.base}/github/complete-email`, { email });
  }

  registerGithubAlternateEmail(body: GithubAlternateEmailRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.base}/github/register-alternate-email`, body);
  }

  oauthAuthorizationUrl(provider: 'google' | 'microsoft' | 'github'): string {
    return `${environment.apiUrl}/oauth2/authorization/${provider}`;
  }
}
