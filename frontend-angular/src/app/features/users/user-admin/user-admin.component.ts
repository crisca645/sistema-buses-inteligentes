import { Component, inject, OnDestroy, OnInit } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialog } from '@angular/material/dialog';
import {
  debounceTime,
  distinctUntilChanged,
  firstValueFrom,
  startWith,
  Subject,
  switchMap,
  takeUntil,
} from 'rxjs';
import { UserWithRolesResponse } from '../../../core/models/api.models';
import { UserRoleApiService } from '../../../core/services/user-role-api.service';
import { UserRolesDialogComponent } from '../user-roles-dialog/user-roles-dialog.component';

@Component({
  selector: 'app-user-admin',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatTableModule,
    MatButtonModule,
    MatChipsModule,
  ],
  templateUrl: './user-admin.component.html',
  styleUrl: './user-admin.component.scss',
})
export class UserAdminComponent implements OnInit, OnDestroy {
  private readonly api = inject(UserRoleApiService);
  private readonly dialog = inject(MatDialog);
  private readonly destroy$ = new Subject<void>();

  search = new FormControl('', { nonNullable: true });
  rows: UserWithRolesResponse[] = [];

  displayedColumns = ['name', 'email', 'roles', 'actions'];

  ngOnInit(): void {
    this.search.valueChanges
      .pipe(
        startWith(this.search.value),
        debounceTime(300),
        distinctUntilChanged(),
        switchMap((q) => this.api.usersWithRoles(q || undefined)),
        takeUntil(this.destroy$),
      )
      .subscribe((list) => {
        this.rows = list;
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  async openRoles(user: UserWithRolesResponse): Promise<void> {
    const ref = this.dialog.open(UserRolesDialogComponent, {
      data: { user },
      width: '640px',
    });
    await firstValueFrom(ref.afterClosed());
    const q = this.search.value;
    this.rows = await firstValueFrom(this.api.usersWithRoles(q || undefined));
  }
}
