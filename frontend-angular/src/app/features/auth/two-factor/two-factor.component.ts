import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { firstValueFrom } from 'rxjs';
import { TwoFactorResponse } from '../../../core/models/api.models';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';
import { SecurityApiService } from '../../../core/services/security-api.service';
import { environment } from '../../../../environments/environment';

interface Pending2FAState {
  sessionId?: string;
  maskedEmail?: string;
  codeExpiresAtMs?: number;
}

@Component({
  selector: 'app-two-factor',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
  ],
  templateUrl: './two-factor.component.html',
  styleUrl: './two-factor.component.scss',
})
export class TwoFactorComponent implements OnInit, OnDestroy {
  private readonly fb = inject(FormBuilder);
  private readonly securityApi = inject(SecurityApiService);
  private readonly auth = inject(AuthService);
  private readonly notify = inject(NotificationService);
  private readonly router = inject(Router);

  sessionId = '';
  maskedEmail = '';

  /** Fin de ventana del código2FA (ms desde epoch, reloj del cliente). */
  codeExpiresAtMs: number | null = null;
  remainingSeconds: number | null = null;
  codeExpired = false;

  private countdownId: ReturnType<typeof setInterval> | null = null;
  /** Evita cancelar por sendBeacon al salir con flujo completado o cancel explícito. */
  private cleanExit = false;

  readonly form = this.fb.nonNullable.group({
    code: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]],
  });

  busy = false;

  ngOnInit(): void {
    let st = history.state as Pending2FAState | undefined;
    if (!st?.sessionId) {
      try {
        const raw = sessionStorage.getItem('pending2fa');
        if (raw) st = JSON.parse(raw) as Pending2FAState;
      } catch {
        /* ignore */
      }
    }
    const sid = st?.sessionId;
    if (!sid) {
      this.notify.info('Inicia sesión de nuevo para recibir un código 2FA.');
      void this.router.navigate(['/login']);
      return;
    }
    this.sessionId = sid;
    this.maskedEmail = st?.maskedEmail ?? '';
    this.codeExpiresAtMs =
      typeof st?.codeExpiresAtMs === 'number' && Number.isFinite(st.codeExpiresAtMs)
        ? st.codeExpiresAtMs
        : null;
    if (this.codeExpiresAtMs == null) {
      this.codeExpiresAtMs = Date.now() + 300 * 1000;
      this.persistPending2fa();
    }
    this.startCountdown();
  }

  ngOnDestroy(): void {
    this.stopCountdown();
    if (this.cleanExit || !this.sessionId) {
      return;
    }
    try {
      const url = `${environment.apiUrl}/security/2fa/cancel`;
      const blob = new Blob([JSON.stringify({ sessionId: this.sessionId })], {
        type: 'application/json',
      });
      navigator.sendBeacon(url, blob);
    } catch {
      /* ignore */
    }
    sessionStorage.removeItem('pending2fa');
  }

  private persistPending2fa(): void {
    if (!this.sessionId) return;
    sessionStorage.setItem(
      'pending2fa',
      JSON.stringify({
        sessionId: this.sessionId,
        maskedEmail: this.maskedEmail,
        codeExpiresAtMs: this.codeExpiresAtMs,
      }),
    );
  }

  private startCountdown(): void {
    this.stopCountdown();
    this.tick();
    this.countdownId = setInterval(() => this.tick(), 1000);
  }

  private stopCountdown(): void {
    if (this.countdownId != null) {
      clearInterval(this.countdownId);
      this.countdownId = null;
    }
  }

  private tick(): void {
    if (this.codeExpiresAtMs == null) {
      this.remainingSeconds = null;
      return;
    }
    const ms = this.codeExpiresAtMs - Date.now();
    if (ms <= 0) {
      this.remainingSeconds = 0;
      this.codeExpired = true;
      this.stopCountdown();
      return;
    }
    this.remainingSeconds = Math.ceil(ms / 1000);
    this.codeExpired = false;
  }

  onCodeInput(ev: Event): void {
    const el = ev.target as HTMLInputElement;
    const v = el.value.replace(/\D/g, '').slice(0, 6);
    this.form.controls.code.setValue(v);
  }

  async verify(): Promise<void> {
    if (this.codeExpired) {
      this.notify.error('El código expiró. Usa «Reenviar código» para obtener uno nuevo.');
      return;
    }
    if (!this.sessionId || this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.busy = true;
    try {
      const res = await firstValueFrom(
        this.securityApi.verify2FA({ sessionId: this.sessionId, code: this.form.controls.code.value }),
      );
      if (res.authenticated && res.token) {
        this.cleanExit = true;
        this.stopCountdown();
        sessionStorage.removeItem('pending2fa');
        this.auth.setToken(res.token);
        this.notify.success(res.message ?? 'Verificación correcta');
        void this.router.navigate(['/dashboard']);
        return;
      }
      if (res.sessionInvalidated) {
        this.cleanExit = true;
        this.stopCountdown();
        sessionStorage.removeItem('pending2fa');
        this.notify.error(res.message ?? 'Sesión inválida');
        void this.router.navigate(['/login']);
        return;
      }
      if (res.message) {
        this.notify.error(res.message);
      }
    } catch (e: unknown) {
      const res = this.parseTwoFactorError(e);
      if (res?.sessionInvalidated) {
        this.cleanExit = true;
        this.stopCountdown();
        sessionStorage.removeItem('pending2fa');
        this.notify.error(res.message ?? 'Sesión inválida');
        void this.router.navigate(['/login']);
        return;
      }
      if (res?.message) {
        this.notify.error(res.message);
        return;
      }
    } finally {
      this.busy = false;
    }
  }

  private parseTwoFactorError(e: unknown): TwoFactorResponse | null {
    if (e instanceof HttpErrorResponse && e.error && typeof e.error === 'object') {
      return e.error as TwoFactorResponse;
    }
    return null;
  }

  async resend(): Promise<void> {
    if (!this.sessionId) return;
    this.busy = true;
    try {
      const res = await firstValueFrom(this.securityApi.resend2FA({ sessionId: this.sessionId }));
      if (res.resent) {
        const sec = res.expiresInSeconds ?? 300;
        this.codeExpiresAtMs = Date.now() + sec * 1000;
        this.codeExpired = false;
        this.form.controls.code.setValue('');
        this.persistPending2fa();
        this.startCountdown();
        this.notify.success(res.message ?? 'Código reenviado');
      } else if (res.sessionInvalidated) {
        this.cleanExit = true;
        this.stopCountdown();
        sessionStorage.removeItem('pending2fa');
        this.notify.error(res.message ?? 'Sesión expirada');
        void this.router.navigate(['/login']);
      } else {
        this.notify.info(res.message ?? 'No se pudo reenviar');
      }
    } catch (e: unknown) {
      const res = this.parseTwoFactorError(e);
      if (res?.sessionInvalidated) {
        this.cleanExit = true;
        this.stopCountdown();
        sessionStorage.removeItem('pending2fa');
        this.notify.error(res.message ?? 'Sesión expirada');
        void this.router.navigate(['/login']);
      } else if (res?.message) {
        this.notify.error(res.message);
      } else {
        this.notify.error('No se pudo reenviar el código');
      }
    } finally {
      this.busy = false;
    }
  }

  async cancel(): Promise<void> {
    if (!this.sessionId) return;
    this.busy = true;
    try {
      await firstValueFrom(this.securityApi.cancel2FA({ sessionId: this.sessionId }));
      this.cleanExit = true;
      this.stopCountdown();
      sessionStorage.removeItem('pending2fa');
      this.notify.info('Verificación cancelada');
      void this.router.navigate(['/login']);
    } catch {
      this.cleanExit = true;
      this.stopCountdown();
      sessionStorage.removeItem('pending2fa');
      void this.router.navigate(['/login']);
    } finally {
      this.busy = false;
    }
  }
}
