import { Component, computed, inject } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatDividerModule } from '@angular/material/divider';
import { MatListModule } from '@angular/material/list';
import { firstValueFrom } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';
import { SecurityApiService } from '../../../core/services/security-api.service';

@Component({
  selector: 'app-profile-settings',
  standalone: true,
  imports: [MatCardModule, MatButtonModule, MatDividerModule, MatListModule],
  templateUrl: './profile-settings.component.html',
  styleUrl: './profile-settings.component.scss',
})
export class ProfileSettingsComponent {
  private readonly auth = inject(AuthService);
  private readonly securityApi = inject(SecurityApiService);
  private readonly notify = inject(NotificationService);
  readonly payload = computed(() => this.auth.payload());
  readonly roles = computed(() => this.auth.roles());

  busy = false;

  async unlinkGoogle(): Promise<void> {
    const ok = confirm(
      '¿Desvincular Google? Necesitas tener contraseña local configurada en el backend.',
    );
    if (!ok) return;
    this.busy = true;
    try {
      const res = await firstValueFrom(this.securityApi.unlinkGoogle());
      this.notify.success(res.message ?? 'Listo');
    } catch {
      /* global */
    } finally {
      this.busy = false;
    }
  }

  async unlinkGithub(): Promise<void> {
    const ok = confirm(
      '¿Desvincular GitHub? Necesitas tener contraseña local configurada en el backend.',
    );
    if (!ok) return;
    this.busy = true;
    try {
      const res = await firstValueFrom(this.securityApi.unlinkGithub());
      this.notify.success(res.message ?? 'Listo');
    } catch {
      /* global */
    } finally {
      this.busy = false;
    }
  }
}
