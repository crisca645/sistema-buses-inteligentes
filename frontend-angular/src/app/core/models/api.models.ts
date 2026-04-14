import { Role } from './role.model';

export interface LoginRequest {
  email: string;
  password: string;
  recaptchaToken: string;
}

export interface Login2FAResponse {
  success?: boolean;
  errorType?: string;
  requires2FA?: boolean;
  sessionId?: string;
  maskedEmail?: string;
  message?: string;
  expiresAt?: string;
  expiresInSeconds?: number;
}

export interface TwoFactorVerifyBody {
  sessionId: string;
  code: string;
}

export interface TwoFactorResponse {
  authenticated?: boolean;
  message?: string;
  token?: string;
  sessionId?: string;
  expiresAt?: string;
  attemptsRemaining?: number;
  sessionInvalidated?: boolean;
  resent?: boolean;
  cancelled?: boolean;
}

export interface RegisterRequest {
  name: string;
  lastname: string;
  email: string;
  password: string;
  confirmPassword: string;
  recaptchaToken: string;
}

export interface MessageResponse {
  message?: string;
}

export interface User {
  id?: string;
  name?: string;
  lastname?: string;
  email?: string;
  password?: string;
  authProvider?: string;
  providerId?: string;
  picture?: string;
  emailVerified?: boolean;
  active?: boolean;
  username?: string;
  address?: string;
  phone?: string;
}

export interface AuthResponse {
  success?: boolean;
  token?: string;
  user?: User;
  userId?: string;
  email?: string;
  name?: string;
  lastname?: string;
  authProvider?: string;
  message?: string;
  newUser?: boolean;
  isNewUser?: boolean;
  requiresAdditionalInfo?: boolean;
  requiresCompleteProfile?: boolean;
  emailRequired?: boolean;
  /** GitHub sin email en BD: pedir email alternativo (OAuth redirect o JSON manual) */
  requiresEmailCompletion?: boolean;
  providerId?: string;
  username?: string;
  picture?: string;
}

export interface GithubAlternateEmailRequest {
  providerId: string;
  username?: string;
  name?: string;
  picture?: string;
  email: string;
}

export interface CompleteProfileResponse extends MessageResponse {
  success?: boolean;
  token?: string;
  userId?: string;
  email?: string;
}

export interface Permission {
  id?: string;
  url?: string;
  method?: string;
  model?: string;
}

export interface RolePermission {
  id?: string;
  role?: Role;
  permission?: Permission;
}

export interface UserRoleByUserResponse {
  userRoleId?: string;
  roleId?: string;
  roleName?: string;
  roleDescription?: string;
}

export interface RoleAvailabilityResponse {
  id?: string;
  name?: string;
  description?: string;
  assigned?: boolean;
}

export interface UserAvailableRolesResponse {
  id?: string;
  name?: string;
  email?: string;
  availableRoles?: RoleAvailabilityResponse[];
}

export interface UserWithRolesResponse {
  id?: string;
  name?: string;
  email?: string;
  roles?: Role[];
}
