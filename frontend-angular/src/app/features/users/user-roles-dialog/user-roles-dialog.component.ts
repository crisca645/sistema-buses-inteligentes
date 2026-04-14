import { HttpErrorResponse } from '@angular/common/http';
import { Component, Inject, inject, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatListModule } from '@angular/material/list';
import { MatButtonModule } from '@angular/material/button';
import { MatDividerModule } from '@angular/material/divider';
import { firstValueFrom } from 'rxjs';
import {
  RoleAvailabilityResponse,
  UserRoleByUserResponse,
  UserWithRolesResponse,
} from '../../../core/models/api.models';
import { NotificationService } from '../../../core/services/notification.service';
import { UserRoleApiService } from '../../../core/services/user-role-api.service';

export interface UserRolesDialogData {
  user: UserWithRolesResponse;
}

@Component({
  selector: 'app-user-roles-dialog',
  standalone: true,
  imports: [MatDialogModule, MatListModule, MatButtonModule, MatDividerModule],
  templateUrl: './user-roles-dialog.component.html',
  styleUrl: './user-roles-dialog.component.scss',
})
export class UserRolesDialogComponent implements OnInit {
  private readonly api = inject(UserRoleApiService);
  private readonly notify = inject(NotificationService);
  readonly dialogRef = inject(MatDialogRef<UserRolesDialogComponent, boolean>);

  private apiErrorMessage(err: unknown, fallback: string): string {
    if (err instanceof HttpErrorResponse && err.error && typeof err.error === 'object') {
      const msg = (err.error as { message?: string }).message;
      if (msg) return msg;
    }
    return fallback;
  }

  current: UserRoleByUserResponse[] = [];
  available: RoleAvailabilityResponse[] = [];
  loading = false;

  constructor(@Inject(MAT_DIALOG_DATA) public data: UserRolesDialogData) {}

  async ngOnInit(): Promise<void> {
    await this.reload();
  }

  get userId(): string {
    return this.data.user.id ?? '';
  }

  async reload(): Promise<void> {
    if (!this.userId) return;
    this.loading = true;
    try {
      this.current = await firstValueFrom(this.api.userRoles(this.userId));
      const pack = await firstValueFrom(this.api.availableRoles(this.userId));
      this.available = pack.availableRoles ?? [];
    } catch {
      this.current = [];
      this.available = [];
    } finally {
      this.loading = false;
    }
  }

  async assign(roleId?: string): Promise<void> {
    if (!this.userId || !roleId) return;
    try {
      await firstValueFrom(this.api.assign(this.userId, roleId));
      this.notify.success('Rol asignado');
      await this.reload();
    } catch (e: unknown) {
      this.notify.error(this.apiErrorMessage(e, 'No se pudo asignar el rol'));
    }
  }

  async remove(userRoleId?: string): Promise<void> {
    if (!userRoleId) return;
    try {
      await firstValueFrom(this.api.remove(userRoleId));
      this.notify.success('Rol removido');
      await this.reload();
    } catch (e: unknown) {
      this.notify.error(this.apiErrorMessage(e, 'No se pudo quitar el rol'));
    }
  }

  close(): void {
    this.dialogRef.close(true);
  }
}
