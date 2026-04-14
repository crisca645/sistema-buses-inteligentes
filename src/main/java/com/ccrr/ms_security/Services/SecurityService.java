package com.ccrr.ms_security.Services;

import com.ccrr.ms_security.Models.LoginRequest;
import com.ccrr.ms_security.Models.Session;
import com.ccrr.ms_security.Models.User;
import com.ccrr.ms_security.Models.UserRole;
import com.ccrr.ms_security.Repositories.UserRepository;
import com.ccrr.ms_security.Repositories.UserRoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
public class SecurityService {

    private static final Logger log = LoggerFactory.getLogger(SecurityService.class);

    public static final String ERROR_TYPE_INVALID_CREDENTIALS = "INVALID_CREDENTIALS";
    public static final String ERROR_TYPE_EMAIL_2FA_FAILED = "EMAIL_2FA_FAILED";

    @Autowired
    private UserRoleRepository theUserRoleRepository;

    @Autowired
    private UserRepository theUserRepository;

    @Autowired
    private EncryptionService theEncryptionService;

    @Autowired
    private JwtService theJwtService;

    @Autowired
    private SessionService theSessionService;

    @Autowired
    private EmailNotificationService theEmailNotificationService;

    @Value("${app.2fa.code.expiration-ms:300000}")
    private Long twoFactorCodeExpirationMs;

    @Value("${jwt.expiration:3600000}")
    private Long jwtExpirationMs;

    @Value("${app.2fa.dev-log-code-only:false}")
    private boolean devLogCodeOnly;

    /**
     * Login con 2FA. Siempre devuelve un mapa con {@code success} (nunca {@code null}).
     * <ul>
     *   <li>{@code success=false}, {@code errorType=INVALID_CREDENTIALS}: credenciales inválidas o datos incompletos.</li>
     *   <li>{@code success=false}, {@code errorType=EMAIL_2FA_FAILED}: sesión 2FA creada pero no se pudo enviar el correo (sesión eliminada).</li>
     *   <li>{@code success=true}: requiere verificación 2FA; incluye {@code sessionId}, {@code maskedEmail}, {@code expiresAt}, etc.</li>
     * </ul>
     */
    public HashMap<String, Object> loginWith2FA(LoginRequest loginRequest) {
        if (loginRequest == null) {
            return loginFailureResponse(ERROR_TYPE_INVALID_CREDENTIALS, "Email o contraseña incorrectos");
        }

        String email = loginRequest.getEmail() != null ? loginRequest.getEmail().trim().toLowerCase() : null;
        String password = loginRequest.getPassword();

        if (!StringUtils.hasText(email) || !StringUtils.hasText(password)) {
            return loginFailureResponse(ERROR_TYPE_INVALID_CREDENTIALS, "Email o contraseña incorrectos");
        }

        User theActualUser = this.theUserRepository.getUserByEmail(email);
        if (theActualUser == null || !theActualUser.getPassword().equals(this.theEncryptionService.convertSHA256(password))) {
            return loginFailureResponse(ERROR_TYPE_INVALID_CREDENTIALS, "Email o contraseña incorrectos");
        }

        String maskedEmail = this.maskEmail(theActualUser.getEmail());

        Session session = new Session();
        session.setUser(theActualUser);
        session.setToken(this.generateSignedToken(theActualUser));
        session.setCode2FA(this.generateTwoFactorCode());
        session.setFailedAttempts(0);
        session.setExpiration(new Date(System.currentTimeMillis() + this.twoFactorCodeExpirationMs));
        session = this.theSessionService.create(session);

        boolean emailSent;
        if (this.devLogCodeOnly) {
            log.warn(
                    "[2FA modo desarrollo] No se envía correo. usuario={} sessionId={} código={}",
                    theActualUser.getEmail(),
                    session.getId(),
                    session.getCode2FA());
            emailSent = true;
        } else {
            emailSent = this.theEmailNotificationService.sendTwoFactorCodeNotification(theActualUser, session.getCode2FA());
        }
        if (!emailSent) {
            this.theSessionService.delete(session.getId());
            return loginFailureResponse(
                    ERROR_TYPE_EMAIL_2FA_FAILED,
                    "No se pudo enviar el código de verificación al correo");
        }

        HashMap<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("requires2FA", true);
        response.put("sessionId", session.getId());
        response.put("maskedEmail", maskedEmail);
        if (this.devLogCodeOnly) {
            response.put("message", "Modo desarrollo: revisa la consola del servidor para el código 2FA");
            response.put("dev2faMode", true);
        } else {
            response.put("message", "Código de verificación enviado al correo");
        }
        response.put("expiresAt", session.getExpiration());
        response.put("expiresInSeconds", this.twoFactorCodeExpirationMs / 1000);
        return response;
    }

    private static HashMap<String, Object> loginFailureResponse(String errorType, String message) {
        HashMap<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("errorType", errorType);
        response.put("message", message);
        return response;
    }

    public HashMap<String, Object> verify2FA(String sessionId, String code) {
        if (!StringUtils.hasText(sessionId) || !StringUtils.hasText(code)) {
            return null;
        }

        sessionId = sessionId.trim();
        code = code.trim();

        if (!code.matches("\\d{6}")) {
            HashMap<String, Object> response = new HashMap<>();
            response.put("authenticated", false);
            response.put("message", "El código debe contener exactamente 6 dígitos numéricos.");
            return response;
        }

        Session session = this.theSessionService.findById(sessionId);
        if (session == null || session.getCode2FA() == null) {
            return null;
        }

        if (session.getExpiration() == null || session.getExpiration().before(new Date())) {
            this.theSessionService.delete(session.getId());
            HashMap<String, Object> response = new HashMap<>();
            response.put("authenticated", false);
            response.put("sessionInvalidated", true);
            response.put("message", "El código expiró. Debe iniciar sesión nuevamente.");
            return response;
        }

        Integer failedAttempts = session.getFailedAttempts() == null ? 0 : session.getFailedAttempts();
        if (failedAttempts >= 3) {
            this.theSessionService.delete(session.getId());
            HashMap<String, Object> response = new HashMap<>();
            response.put("authenticated", false);
            response.put("sessionInvalidated", true);
            response.put("message", "La sesión de verificación ha sido invalidada. Debe iniciar sesión nuevamente.");
            return response;
        }

        if (!session.getCode2FA().equals(code)) {
            failedAttempts = failedAttempts + 1;
            session.setFailedAttempts(failedAttempts);

            if (failedAttempts >= 3) {
                this.theSessionService.delete(session.getId());
                HashMap<String, Object> response = new HashMap<>();
                response.put("authenticated", false);
                response.put("sessionInvalidated", true);
                response.put("attemptsRemaining", 0);
                response.put("message", "Ha superado el máximo de intentos. Debe iniciar sesión nuevamente.");
                return response;
            }

            this.theSessionService.update(session.getId(), session);
            int attemptsRemaining = 3 - failedAttempts;

            HashMap<String, Object> response = new HashMap<>();
            response.put("authenticated", false);
            response.put("attemptsRemaining", attemptsRemaining);
            response.put("message", "Código incorrecto. Intentos restantes: " + attemptsRemaining);
            return response;
        }

        session.setCode2FA(null);
        session.setFailedAttempts(0);
        session.setExpiration(new Date(System.currentTimeMillis() + this.jwtExpirationMs));
        this.theSessionService.update(session.getId(), session);

        HashMap<String, Object> response = new HashMap<>();
        response.put("authenticated", true);
        response.put("message", "Autenticación completada correctamente");
        response.put("token", session.getToken());
        response.put("sessionId", session.getId());
        response.put("expiresAt", session.getExpiration());
        return response;
    }

    public HashMap<String, Object> resend2FA(String sessionId) {
        if (!StringUtils.hasText(sessionId)) {
            return null;
        }

        sessionId = sessionId.trim();
        Session session = this.theSessionService.findById(sessionId);

        if (session == null || session.getUser() == null) {
            return null;
        }

        if (session.getCode2FA() == null) {
            HashMap<String, Object> response = new HashMap<>();
            response.put("resent", false);
            response.put("message", "La sesión ya fue autenticada. No es posible reenviar un código 2FA.");
            return response;
        }

        if (session.getExpiration() == null || session.getExpiration().before(new Date())) {
            this.theSessionService.delete(session.getId());
            HashMap<String, Object> response = new HashMap<>();
            response.put("resent", false);
            response.put("sessionInvalidated", true);
            response.put("message", "La sesión parcial expiró. Debe iniciar sesión nuevamente.");
            return response;
        }

        User user = session.getUser();

        String previousCode = session.getCode2FA();
        Date previousExpiration = session.getExpiration();
        Integer previousFailedAttempts = session.getFailedAttempts();

        String newCode = this.generateTwoFactorCode();
        session.setCode2FA(newCode);
        session.setFailedAttempts(0);
        session.setExpiration(new Date(System.currentTimeMillis() + this.twoFactorCodeExpirationMs));
        this.theSessionService.update(session.getId(), session);

        boolean emailSent = this.theEmailNotificationService.sendTwoFactorCodeNotification(user, newCode);
        if (!emailSent) {
            session.setCode2FA(previousCode);
            session.setExpiration(previousExpiration);
            session.setFailedAttempts(previousFailedAttempts);
            this.theSessionService.update(session.getId(), session);
            return null;
        }

        HashMap<String, Object> response = new HashMap<>();
        response.put("resent", true);
        response.put("sessionId", session.getId());
        response.put("message", "Se ha reenviado un nuevo código de verificación a su correo.");
        response.put("expiresAt", session.getExpiration());
        response.put("expiresInSeconds", this.twoFactorCodeExpirationMs / 1000);
        return response;
    }

    public HashMap<String, Object> cancel2FA(String sessionId) {
        if (!StringUtils.hasText(sessionId)) {
            return null;
        }

        sessionId = sessionId.trim();
        Session session = this.theSessionService.findById(sessionId);

        if (session == null) {
            return null;
        }

        if (session.getCode2FA() == null) {
            HashMap<String, Object> response = new HashMap<>();
            response.put("cancelled", false);
            response.put("message", "La sesión ya fue autenticada y no puede cancelarse desde este endpoint.");
            return response;
        }

        this.theSessionService.delete(sessionId);

        HashMap<String, Object> response = new HashMap<>();
        response.put("cancelled", true);
        response.put("sessionInvalidated", true);
        response.put("message", "La sesión parcial de autenticación ha sido invalidada.");
        return response;
    }

    public User findOrCreateGoogleUser(String email, String name, String lastname) {
        if (!StringUtils.hasText(email)) {
            return null;
        }

        String normalizedEmail = email.trim().toLowerCase();
        User existingUser = theUserRepository.getUserByEmail(normalizedEmail);
        if (existingUser != null) {
            return existingUser;
        }

        User newUser = new User();
        newUser.setEmail(normalizedEmail);
        newUser.setName(StringUtils.hasText(name) ? name : "Usuario");
        newUser.setLastname(StringUtils.hasText(lastname) ? lastname : "OAuth");
        newUser.setPassword(generateOAuthTechnicalPassword());
        newUser.setAuthProvider("GOOGLE");

        return theUserRepository.save(newUser);
    }

    /**
     * Contraseña técnica no usable para login por email (OAuth y cuentas creadas por proveedor).
     * Misma estrategia para Google, Microsoft y GitHub.
     */
    public String generateOAuthTechnicalPassword() {
        return theEncryptionService.convertSHA256(UUID.randomUUID().toString());
    }

    public User findOrCreateOAuthUser(String email,
                                      String name,
                                      String lastname,
                                      String authProvider,
                                      String providerId,
                                      String picture,
                                      Boolean emailVerified) {
        if (!StringUtils.hasText(email)) {
            return null;
        }

        String normalizedEmail = email.trim().toLowerCase();
        User existingUser = theUserRepository.getUserByEmail(normalizedEmail);

        if (existingUser != null) {
            existingUser.setName(StringUtils.hasText(name) ? name : existingUser.getName());
            existingUser.setLastname(StringUtils.hasText(lastname) ? lastname : existingUser.getLastname());
            existingUser.setAuthProvider(authProvider);
            existingUser.setProviderId(providerId);
            existingUser.setPicture(picture);
            existingUser.setEmailVerified(emailVerified);
            existingUser.setActive(true);
            return theUserRepository.save(existingUser);
        }

        User newUser = new User();
        newUser.setEmail(normalizedEmail);
        newUser.setName(StringUtils.hasText(name) ? name : "Usuario");
        newUser.setLastname(StringUtils.hasText(lastname) ? lastname : "OAuth");
        newUser.setPassword(generateOAuthTechnicalPassword());
        newUser.setAuthProvider(authProvider);
        newUser.setProviderId(providerId);
        newUser.setPicture(picture);
        newUser.setEmailVerified(emailVerified);
        newUser.setActive(true);

        return theUserRepository.save(newUser);
    }

    public boolean isProfileIncomplete(User user) {
        if (user == null) {
            return true;
        }
        return !StringUtils.hasText(user.getAddress()) || !StringUtils.hasText(user.getPhone());
    }

    public HashMap<String, Object> createProfileIncompleteResponse(User user) {
        HashMap<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("requiresCompleteProfile", true);
        response.put("message", "Debe completar información adicional requerida");
        response.put("userId", user != null ? user.getId() : null);
        response.put("email", user != null ? user.getEmail() : null);
        response.put("name", user != null ? user.getName() : null);
        response.put("lastname", user != null ? user.getLastname() : null);
        response.put("authProvider", user != null ? user.getAuthProvider() : null);
        return response;
    }

    public HashMap<String, Object> completeProfileAndGenerateToken(String userId, String address, String phone) {
        if (!StringUtils.hasText(userId) || !StringUtils.hasText(address) || !StringUtils.hasText(phone)) {
            return null;
        }

        User user = this.findById(userId.trim());
        if (user == null) {
            return null;
        }

        user.setAddress(address.trim());
        user.setPhone(phone.trim());
        user = this.save(user);

        String token = this.generateTokenForUser(user);
        HashMap<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Perfil completado correctamente");
        response.put("token", token);
        response.put("userId", user.getId());
        response.put("email", user.getEmail());
        return response;
    }

    public String generateTokenForUser(User user) {
        String token = this.generateSignedToken(user);
        Session newSession = new Session();
        newSession.setToken(token);
        newSession.setUser(user);
        newSession.setCode2FA(null);
        newSession.setFailedAttempts(0);
        newSession.setExpiration(new Date(System.currentTimeMillis() + this.jwtExpirationMs));
        this.theSessionService.create(newSession);
        return token;
    }

    private String generateSignedToken(User user) {
        List<UserRole> userRoles = theUserRoleRepository.getRolesByUser(user.getId());
        String roleName = userRoles.stream()
                .filter(ur -> ur.getRole() != null)
                .map(ur -> ur.getRole().getName())
                .collect(Collectors.joining(","));
        if (roleName.isEmpty()) {
            roleName = "USER";
        }
        return theJwtService.generateToken(user, roleName);
    }

    private String generateTwoFactorCode() {
        int randomNumber = ThreadLocalRandom.current().nextInt(100000, 1000000);
        return String.valueOf(randomNumber);
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "";
        }

        String[] parts = email.split("@", 2);
        String localPart = parts[0];
        String domainPart = parts[1];

        String maskedLocal;
        if (localPart.isEmpty()) {
            maskedLocal = "***";
        } else if (localPart.length() <= 1) {
            maskedLocal = localPart.charAt(0) + "***";
        } else {
            maskedLocal = localPart.substring(0, 2) + "***";
        }

        int dotIndex = domainPart.lastIndexOf('.');
        String domainExtension = dotIndex > 0 ? domainPart.substring(dotIndex) : "";

        return maskedLocal + "@***" + domainExtension;
    }

    public User findById(String id) {
        return theUserRepository.findById(id).orElse(null);
    }

    public User save(User user) {
        return theUserRepository.save(user);
    }

    public User getUserFromToken(String token) {
        User tokenUser = theJwtService.getUserFromToken(token);
        if (tokenUser == null || tokenUser.getId() == null) {
            return null;
        }
        return theUserRepository.findById(tokenUser.getId()).orElse(null);
    }

    public String encryptPassword(String password) {
        return theEncryptionService.convertSHA256(password);
    }
}