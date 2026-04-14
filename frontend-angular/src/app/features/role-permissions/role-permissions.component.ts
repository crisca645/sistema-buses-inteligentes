import { Component, inject, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { firstValueFrom } from 'rxjs';
import { Permission, RolePermission } from '../../core/models/api.models';
import { Role } from '../../core/models/role.model';
import { PermissionsApiService } from '../../core/services/permissions-api.service';
import { RolePermissionApiService } from '../../core/services/role-permission-api.service';
import { RolesApiService } from '../../core/services/roles-api.service';
import { NotificationService } from '../../core/services/notification.service';

@Component({
  selector: 'app-role-permissions',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatSelectModule,
    MatTableModule,
    MatButtonModule,
    MatChipsModule,
    MatIconModule,
  ],
  templateUrl: './role-permissions.component.html',
  styleUrl: './role-permissions.component.scss',
})
export class RolePermissionsComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly rolesApi = inject(RolesApiService);
  private readonly permissionsApi = inject(PermissionsApiService);
  private readonly rpApi = inject(RolePermissionApiService);
  private readonly notify = inject(NotificationService);

  readonly form = this.fb.nonNullable.group({ roleId: [''] });

  roles: Role[] = [];
  allPermissions: Permission[] = [];
  assigned: RolePermission[] = [];
  displayedColumns = ['model', 'method', 'url', 'actions'];
  busy = false;

  async ngOnInit(): Promise<void> {
    try {
      this.roles = await firstValueFrom(this.rolesApi.list());
      this.allPermissions = await firstValueFrom(this.permissionsApi.list());
    } catch {
      /* global */
    }
  }

  async onRoleChange(): Promise<void> {
    const id = this.form.controls.roleId.value;
    if (!id) {
      this.assigned = [];
      return;
    }
    this.busy = true;
    try {
      this.assigned = await firstValueFrom(this.rpApi.byRole(id));
    } catch {
      this.assigned = [];
    } finally {
      this.busy = false;
    }
  }

  isAssigned(permissionId?: string): RolePermission | undefined {
    if (!permissionId) return undefined;
    return this.assigned.find((a) => a.permission?.id === permissionId);
  }

  async assign(permission: Permission): Promise<void> {
    const roleId = this.form.controls.roleId.value;
    if (!roleId || !permission.id) return;
    try {
      await firstValueFrom(this.rpApi.assign(roleId, permission.id));
      this.notify.success('Permiso asignado');
      await this.onRoleChange();
    } catch {
      /* global */
    }
  }

  async remove(rp: RolePermission): Promise<void> {
    if (!rp.id) return;
    try {
      await firstValueFrom(this.rpApi.remove(rp.id));
      this.notify.success('Permiso removido');
      await this.onRoleChange();
    } catch {
      /* global */
    }
  }
}
