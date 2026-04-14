import { Component, inject, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { firstValueFrom } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';
import { SecurityApiService } from '../../../core/services/security-api.service';

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
export class TwoFactorComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly securityApi = inject(SecurityApiService);
  private readonly auth = inject(AuthService);
  private readonly notify = inject(NotificationService);
  private readonly router = inject(Router);

  sessionId = '';
  maskedEmail = '';

  readonly form = this.fb.nonNullable.group({
    code: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]],
  });

  busy = false;

  ngOnInit(): void {
    let st = history.state as { sessionId?: string; maskedEmail?: string } | undefined;
    if (!st?.sessionId) {
      try {
        const raw = sessionStorage.getItem('pending2fa');
        if (raw) st = JSON.parse(raw) as { sessionId?: string; maskedEmail?: string };
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
  }

  async verify(): Promise<void> {
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
        sessionStorage.removeItem('pending2fa');
        this.auth.setToken(res.token);
        this.notify.success(res.message ?? 'Verificación correcta');
        void this.router.navigate(['/dashboard']);
        return;
      }
      if (res.sessionInvalidated) {
        this.notify.error(res.message ?? 'Sesión inválida');
        void this.router.navigate(['/login']);
        return;
      }
      this.notify.error(res.message ?? 'Código incorrecto');
    } catch {
      /* 401 handled */
    } finally {
      this.busy = false;
    }
  }

  async resend(): Promise<void> {
    if (!this.sessionId) return;
    this.busy = true;
    try {
      const res = await firstValueFrom(this.securityApi.resend2FA({ sessionId: this.sessionId }));
      if (res.resent) {
        this.notify.success(res.message ?? 'Código reenviado');
      } else if (res.sessionInvalidated) {
        this.notify.error(res.message ?? 'Sesión expirada');
        void this.router.navigate(['/login']);
      } else {
        this.notify.info(res.message ?? 'No se pudo reenviar');
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
      sessionStorage.removeItem('pending2fa');
      this.notify.info('Verificación cancelada');
      void this.router.navigate(['/login']);
    } finally {
      this.busy = false;
    }
  }
}
