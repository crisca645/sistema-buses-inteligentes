import { Component, inject } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { firstValueFrom } from 'rxjs';
import { NotificationService } from '../../../core/services/notification.service';
import { RecaptchaService } from '../../../core/services/recaptcha.service';
import { UsersApiService } from '../../../core/services/users-api.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
  ],
  templateUrl: './register.component.html',
  styleUrl: './register.component.scss',
})
export class RegisterComponent {
  private readonly fb = inject(FormBuilder);
  private readonly usersApi = inject(UsersApiService);
  private readonly recaptcha = inject(RecaptchaService);
  private readonly notify = inject(NotificationService);

  readonly recaptchaConfigured = this.recaptcha.isConfigured();

  readonly form = this.fb.nonNullable.group(
    {
      name: ['', [Validators.required, Validators.minLength(2)]],
      lastname: ['', [Validators.required, Validators.minLength(2)]],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', [Validators.required]],
    },
    { validators: [RegisterComponent.passwordsMatch] },
  );

  submitting = false;

  private static passwordsMatch(control: AbstractControl): ValidationErrors | null {
    const p = control.get('password')?.value;
    const c = control.get('confirmPassword')?.value;
    if (p && c && p !== c) {
      return { mismatch: true };
    }
    return null;
  }

  async submit(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    if (!this.recaptchaConfigured) {
      this.notify.error('Configura recaptchaSiteKey en environment.ts');
      return;
    }
    this.submitting = true;
    try {
      const token = await this.recaptcha.execute('register');
      const v = this.form.getRawValue();
      const res = await firstValueFrom(
        this.usersApi.register({
          name: v.name,
          lastname: v.lastname,
          email: v.email,
          password: v.password,
          confirmPassword: v.confirmPassword,
          recaptchaToken: token,
        }),
      );
      this.notify.success(res.message ?? 'Registro exitoso');
    } catch {
      /* interceptor / error */
    } finally {
      this.submitting = false;
    }
  }
}
