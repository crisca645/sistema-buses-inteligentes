export const environment = {
  production: false,
  /** Backend ms-security (Spring Boot) */
  apiUrl: 'http://localhost:8383',
  /**
   * Clave pública reCAPTCHA v3 (debe coincidir con recaptcha.site-key del backend).
   * Si queda vacía, el login/registro/recuperación mostrarán aviso y no podrán enviar token válido.
   */
  recaptchaSiteKey: '6Ldvp5EsAAAAAKZwizt0IvHq36UErlrxnITP0cqb',
};
