import { Component, Inject, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { Role } from '../../../core/models/role.model';

export interface RoleDialogData {
  role?: Role | null;
}

@Component({
  selector: 'app-role-dialog',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
  ],
  template: `
    <h2 mat-dialog-title>{{ data.role?.id ? 'Editar rol' : 'Nuevo rol' }}</h2>
    <form mat-dialog-content [formGroup]="form" (ngSubmit)="save()">
      <mat-form-field appearance="outline" class="full">
        <mat-label>Nombre (mín. 3 caracteres)</mat-label>
        <input matInput formControlName="name" />
      </mat-form-field>
      <mat-form-field appearance="outline" class="full">
        <mat-label>Descripción (mín. 5 caracteres)</mat-label>
        <textarea matInput rows="3" formControlName="description"></textarea>
      </mat-form-field>
    </form>
    <div mat-dialog-actions align="end">
      <button mat-button type="button" mat-dialog-close>Cancelar</button>
      <button mat-flat-button color="primary" type="button" (click)="save()">Guardar</button>
    </div>
  `,
  styles: [
    `
      .full {
        width: 100%;
      }
      form {
        display: flex;
        flex-direction: column;
        gap: 8px;
        min-width: min(420px, 92vw);
      }
    `,
  ],
})
export class RoleDialogComponent {
  private readonly fb = inject(FormBuilder);
  readonly dialogRef = inject(MatDialogRef<RoleDialogComponent, Role | undefined>);

  readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.minLength(3)]],
    description: ['', [Validators.required, Validators.minLength(5)]],
  });

  constructor(@Inject(MAT_DIALOG_DATA) public data: RoleDialogData) {
    if (data.role?.name) {
      this.form.patchValue({
        name: data.role.name ?? '',
        description: data.role.description ?? '',
      });
    }
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    this.dialogRef.close({
      id: this.data.role?.id,
      name: v.name,
      description: v.description,
    });
  }
}
