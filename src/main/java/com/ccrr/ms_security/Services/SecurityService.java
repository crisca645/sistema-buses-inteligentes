package com.ccrr.ms_security.Services;

import com.ccrr.ms_security.Models.Session;
import com.ccrr.ms_security.Models.User;
import com.ccrr.ms_security.Repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class SecurityService {

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

    public String login(User theNewUser) {
        String token = null;
        User theActualUser = this.theUserRepository.getUserByEmail(theNewUser.getEmail());

        if (theActualUser != null &&
                theActualUser.getPassword().equals(
                        this.theEncryptionService.convertSHA256(theNewUser.getPassword()))) {
            token = this.theJwtService.generateToken(theActualUser);
            return token;
        } else {
            return token;
        }
    }

    public HashMap<String, Object> loginWith2FA(User theNewUser) {
        HashMap<String, Object> response = new HashMap<>();

        User theActualUser = this.theUserRepository.getUserByEmail(theNewUser.getEmail());

        if (theActualUser != null &&
                theActualUser.getPassword().equals(
                        this.theEncryptionService.convertSHA256(theNewUser.getPassword()))) {

            String token = this.theJwtService.generateToken(theActualUser);
            String maskedEmail = this.maskEmail(theActualUser.getEmail());

            Session session = new Session();
            session.setUser(theActualUser);
            session.setToken(token);
            session.setCode2FA(this.generateTwoFactorCode());
            session.setFailedAttempts(0);
            session.setExpiration(new Date(System.currentTimeMillis() + this.twoFactorCodeExpirationMs));

            session = this.theSessionService.create(session);

            this.theEmailNotificationService.sendTwoFactorCodeNotification(
                    theActualUser,
                    session.getCode2FA()
            );

            response.put("token", token);
            response.put("requires2FA", true);
            response.put("sessionId", session.getId());
            response.put("maskedEmail", maskedEmail);
            response.put("message", "Ingrese el código de 6 dígitos enviado a su email " + maskedEmail);
            response.put("expiresAt", session.getExpiration());
            response.put("expiresInSeconds", this.twoFactorCodeExpirationMs / 1000);

            return response;
        }

        return null;
    }

    public HashMap<String, Object> verify2FA(String sessionId, String code) {
        if (sessionId == null || sessionId.isEmpty() || code == null || code.isEmpty()) {
            return null;
        }

        Session session = this.theSessionService.findById(sessionId);

        if (session == null) {
            return null;
        }

        if (session.getCode2FA() == null) {
            return null;
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

        HashMap<String, Object> response = new HashMap<>();
        response.put("authenticated", true);
        response.put("message", "Autenticación completada correctamente");
        response.put("token", session.getToken());
        response.put("sessionId", session.getId());

        return response;
    }

    public HashMap<String, Object> resend2FA(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            return null;
        }

        Session session = this.theSessionService.findById(sessionId);

        if (session == null || session.getUser() == null) {
            return null;
        }

        User user = session.getUser();
        String newCode = this.generateTwoFactorCode();

        session.setCode2FA(newCode);
        session.setFailedAttempts(0);
        session.setExpiration(new Date(System.currentTimeMillis() + this.twoFactorCodeExpirationMs));

        this.theSessionService.update(session.getId(), session);

        this.theEmailNotificationService.sendTwoFactorCodeNotification(user, newCode);

        HashMap<String, Object> response = new HashMap<>();
        response.put("resent", true);
        response.put("sessionId", session.getId());
        response.put("message", "Se ha reenviado un nuevo código de verificación a su correo.");
        response.put("expiresAt", session.getExpiration());
        response.put("expiresInSeconds", this.twoFactorCodeExpirationMs / 1000);

        return response;
    }

    public HashMap<String, Object> cancel2FA(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            return null;
        }

        Session session = this.theSessionService.findById(sessionId);

        if (session == null) {
            return null;
        }

        this.theSessionService.delete(sessionId);

        HashMap<String, Object> response = new HashMap<>();
        response.put("cancelled", true);
        response.put("sessionInvalidated", true);
        response.put("message", "La sesión parcial de autenticación ha sido invalidada.");

        return response;
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
        if (localPart.length() <= 2) {
            maskedLocal = localPart.substring(0, 1) + "***";
        } else {
            maskedLocal = localPart.substring(0, 2) + "***";
        }

        int dotIndex = domainPart.lastIndexOf(".");
        String domainName = dotIndex > 0 ? domainPart.substring(0, dotIndex) : domainPart;
        String domainExtension = dotIndex > 0 ? domainPart.substring(dotIndex) : "";

        String maskedDomain;
        if (domainName.isEmpty()) {
            maskedDomain = "***";
        } else {
            maskedDomain = domainName.substring(0, 1) + "***";
        }

        return maskedLocal + "@" + maskedDomain + domainExtension;
    }
}