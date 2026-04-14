import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, OnInit } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatToolbarModule } from '@angular/material/toolbar';
import { firstValueFrom } from 'rxjs';
import { Role } from '../../../core/models/role.model';
import { RolesApiService } from '../../../core/services/roles-api.service';
import { NotificationService } from '../../../core/services/notification.service';
import { RoleDialogComponent } from '../role-dialog/role-dialog.component';

@Component({
  selector: 'app-role-list',
  standalone: true,
  imports: [
    MatCardModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatDialogModule,
    MatToolbarModule,
  ],
  templateUrl: './role-list.component.html',
  styleUrl: './role-list.component.scss',
})
export class RoleListComponent implements OnInit {
  private readonly rolesApi = inject(RolesApiService);
  private readonly dialog = inject(MatDialog);
  private readonly notify = inject(NotificationService);

  private apiErrorMessage(err: unknown, fallback: string): string {
    if (err instanceof HttpErrorResponse && err.error && typeof err.error === 'object') {
      const msg = (err.error as { message?: string }).message;
      if (msg) return msg;
    }
    return fallback;
  }

  displayedColumns = ['name', 'description', 'actions'];
  roles: Role[] = [];
  loading = false;

  async ngOnInit(): Promise<void> {
    await this.refresh();
  }

  async refresh(): Promise<void> {
    this.loading = true;
    try {
      this.roles = await firstValueFrom(this.rolesApi.list());
    } finally {
      this.loading = false;
    }
  }

  async openCreate(): Promise<void> {
    const ref = this.dialog.open(RoleDialogComponent, { data: { role: null } });
    const result = await firstValueFrom(ref.afterClosed());
    if (!result) return;
    try {
      await firstValueFrom(this.rolesApi.create(result));
      this.notify.success('Rol creado');
      await this.refresh();
    } catch (e: unknown) {
      this.notify.error(this.apiErrorMessage(e, 'No se pudo crear el rol'));
    }
  }

  async openEdit(role: Role): Promise<void> {
    const ref = this.dialog.open(RoleDialogComponent, { data: { role } });
    const result = await firstValueFrom(ref.afterClosed());
    if (!result?.id) return;
    try {
      await firstValueFrom(this.rolesApi.update(result.id, result));
      this.notify.success('Rol actualizado');
      await this.refresh();
    } catch (e: unknown) {
      this.notify.error(this.apiErrorMessage(e, 'No se pudo actualizar el rol'));
    }
  }

  async remove(role: Role): Promise<void> {
    if (!role.id) return;
    if (!confirm(`¿Eliminar el rol "${role.name}"?`)) return;
    try {
      await firstValueFrom(this.rolesApi.delete(role.id));
      this.notify.success('Rol eliminado');
      await this.refresh();
    } catch (e: unknown) {
      this.notify.error(
        this.apiErrorMessage(e, 'No se pudo eliminar el rol'),
      );
    }
  }
}
