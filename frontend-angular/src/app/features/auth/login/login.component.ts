import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, OnInit } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatDividerModule } from '@angular/material/divider';
import { MatIconModule } from '@angular/material/icon';
import { MatExpansionModule } from '@angular/material/expansion';
import { AuthResponse, Login2FAResponse } from '../../../core/models/api.models';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';
import { RecaptchaService } from '../../../core/services/recaptcha.service';
import { SecurityApiService } from '../../../core/services/security-api.service';
import { environment } from '../../../../environments/environment';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    FormsModule,
    RouterLink,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatDividerModule,
    MatIconModule,
    MatExpansionModule,
  ],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
})
export class LoginComponent implements OnInit {
  readonly environment = environment;

  private readonly fb = inject(FormBuilder);
  private readonly securityApi = inject(SecurityApiService);
  private readonly recaptcha = inject(RecaptchaService);
  private readonly notify = inject(NotificationService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly auth = inject(AuthService);

  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]],
  });

  readonly recaptchaConfigured = this.recaptcha.isConfigured();
  submitting = false;
  pasteJson = '';

  ngOnInit(): void {
    const oauthError = this.route.snapshot.queryParamMap.get('oauthError');
    if (oauthError) {
      this.notify.error(oauthError);
    }
  }

  oauthGoogle(): void {
    window.location.href = this.securityApi.oauthAuthorizationUrl('google');
  }

  oauthMicrosoft(): void {
    window.location.href = this.securityApi.oauthAuthorizationUrl('microsoft');
  }

  oauthGithub(): void {
    window.location.href = this.securityApi.oauthAuthorizationUrl('github');
  }

  applyOAuthJson(): void {
    const raw = this.pasteJson?.trim();
    if (!raw) {
      this.notify.error('Pega el JSON que devolvió el backend tras OAuth.');
      return;
    }
    try {
      const data = JSON.parse(raw) as AuthResponse & { token?: string; message?: string };
      this.handleAuthResponse(data);
    } catch {
      this.notify.error('JSON inválido. Copia la respuesta completa del servidor.');
    }
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    if (!this.recaptchaConfigured) {
      this.notify.error(
        'Configura recaptchaSiteKey en src/environments/environment.ts (clave pública v3 que coincida con el backend).',
      );
      return;
    }

    this.submitting = true;
    const { email, password } = this.form.getRawValue();

    void (async () => {
      try {
        const recaptchaToken = await this.recaptcha.execute('login');
        const res = await firstValueFrom(this.securityApi.login({ email, password, recaptchaToken }));
        this.onLoginResponse(res as Login2FAResponse & { token?: string; requires2FA?: boolean });
      } catch (err: unknown) {
        this.handleLoginError(err);
      } finally {
        this.submitting = false;
      }
    })();
  }

  private handleLoginError(err: unknown): void {
    if (err instanceof HttpErrorResponse) {
      if (err.status === 0) {
        return;
      }
      const body = err.error;
      if (body && typeof body === 'object' && 'message' in body) {
        const msg = (body as { message?: unknown }).message;
        if (msg != null && String(msg).trim() !== '') {
          this.notify.error(String(msg));
          return;
        }
      }
      this.notify.error(`Error del servidor (${err.status}). Revisa la consola de red.`);
      return;
    }
    if (err instanceof Error) {
      this.notify.error(err.message || 'No se pudo obtener reCAPTCHA. Recarga la página e inténtalo de nuevo.');
      return;
    }
    this.notify.error('No se pudo completar el inicio de sesión');
  }

  private onLoginResponse(res: Login2FAResponse & { token?: string; message?: string }): void {
    if (!res) return;

    if (res.success === false) {
      this.notify.error(res.message ?? 'No se pudo iniciar sesión');
      return;
    }

    if (res.requires2FA && res.sessionId) {
      sessionStorage.setItem(
        'pending2fa',
        JSON.stringify({ sessionId: res.sessionId, maskedEmail: res.maskedEmail ?? '' }),
      );
      void this.router.navigate(['/two-factor'], {
        state: { sessionId: res.sessionId, maskedEmail: res.maskedEmail ?? '' },
      });
      this.notify.info(res.message ?? 'Verifica el código enviado a tu correo.');
      return;
    }

    if (res.token) {
      this.auth.setToken(res.token);
      void this.router.navigate(['/dashboard']);
      this.notify.success('Bienvenido');
    }
  }

  private handleAuthResponse(data: AuthResponse): void {
    if (data.token) {
      this.auth.setToken(data.token);
    }

    if (data.requiresEmailCompletion && data.providerId) {
      void this.router.navigate(['/complete-github-email'], {
        queryParams: {
          requiresEmailCompletion: '1',
          providerId: data.providerId,
          username: data.username ?? '',
          name: data.name ?? '',
          picture: data.picture ?? '',
        },
      });
      if (data.message) {
        this.notify.info(data.message);
      }
      return;
    }

    if (data.emailRequired) {
      void this.router.navigate(['/complete-github-email']);
      return;
    }

    const requiresCompleteProfile =
      data.requiresCompleteProfile === true || data.requiresAdditionalInfo === true;
    const profileUserId = data.userId ?? data.user?.id;

    if (requiresCompleteProfile && profileUserId) {
      void this.router.navigate(['/complete-profile'], {
        queryParams: { userId: profileUserId },
      });
      return;
    }

    if (data.token) {
      void this.router.navigate(['/dashboard']);
      this.notify.success('Sesión iniciada correctamente');
      return;
    }

    this.notify.error(data.message ?? 'No se pudo procesar la respuesta OAuth');
  }
}
