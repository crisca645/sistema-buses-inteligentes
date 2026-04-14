export interface JwtPayload {
  id?: string;
  name?: string;
  email?: string;
  /** Login de GitHub u otro proveedor cuando aplica */
  username?: string;
  role?: string;
  sub?: string;
  exp?: number;
  iat?: number;
}
