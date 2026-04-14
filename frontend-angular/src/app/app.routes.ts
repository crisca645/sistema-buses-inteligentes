import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { guestGuard } from './core/guards/guest.guard';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'login' },

  {
    path: 'login',
    canActivate: [guestGuard],
    loadComponent: () =>
      import('./features/auth/login/login.component').then((m) => m.LoginComponent),
  },
  {
    path: 'register',
    canActivate: [guestGuard],
    loadComponent: () =>
      import('./features/auth/register/register.component').then((m) => m.RegisterComponent),
  },
  {
    path: 'forgot-password',
    canActivate: [guestGuard],
    loadComponent: () =>
      import('./features/auth/forgot-password/forgot-password.component').then(
        (m) => m.ForgotPasswordComponent,
      ),
  },
  {
    path: 'reset-password',
    canActivate: [guestGuard],
    loadComponent: () =>
      import('./features/auth/reset-password/reset-password.component').then(
        (m) => m.ResetPasswordComponent,
      ),
  },
  {
    path: 'two-factor',
    loadComponent: () =>
      import('./features/auth/two-factor/two-factor.component').then((m) => m.TwoFactorComponent),
  },
  {
    path: 'complete-github-email',
    loadComponent: () =>
      import('./features/auth/complete-github-email/complete-github-email.component').then(
        (m) => m.CompleteGithubEmailComponent,
      ),
  },
  {
    path: 'complete-profile',
    loadComponent: () =>
      import('./features/auth/complete-profile/complete-profile.component').then(
        (m) => m.CompleteProfileComponent,
      ),
  },
  {
    path: 'oauth-success',
    loadComponent: () =>
      import('./features/auth/oauth-success/oauth-success.component').then(
        (m) => m.OauthSuccessComponent,
      ),
  },

  {
    path: '',
    loadComponent: () =>
      import('./layout/main-layout/main-layout.component').then((m) => m.MainLayoutComponent),
    canActivate: [authGuard],
    children: [
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./features/dashboard/dashboard.component').then((m) => m.DashboardComponent),
      },
      {
        path: 'roles',
        loadComponent: () =>
          import('./features/roles/role-list/role-list.component').then((m) => m.RoleListComponent),
      },
      {
        path: 'role-permissions',
        loadComponent: () =>
          import('./features/role-permissions/role-permissions.component').then(
            (m) => m.RolePermissionsComponent,
          ),
      },
      {
        path: 'users',
        loadComponent: () =>
          import('./features/users/user-admin/user-admin.component').then((m) => m.UserAdminComponent),
      },
      {
        path: 'profile',
        loadComponent: () =>
          import('./features/profile/profile-settings/profile-settings.component').then(
            (m) => m.ProfileSettingsComponent,
          ),
      },
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
    ],
  },

  {
    path: 'forbidden',
    loadComponent: () =>
      import('./features/forbidden/forbidden.component').then((m) => m.ForbiddenComponent),
  },

  { path: '**', redirectTo: 'login' },
];
