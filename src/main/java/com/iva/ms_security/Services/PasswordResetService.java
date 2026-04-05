package com.iva.ms_security.Services;

import com.iva.ms_security.Models.PasswordResetToken;
import com.iva.ms_security.Models.User;
import com.iva.ms_security.Repositories.PasswordResetTokenRepository;
import com.iva.ms_security.Repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class PasswordResetService {

    @Autowired
    private UserRepository theUserRepository;

    @Autowired
    private PasswordResetTokenRepository theTokenRepository;

    // Solicitar recuperación de contraseña
    public String forgotPassword(String email) {
        User user = this.theUserRepository.getUserByEmail(email.trim().toLowerCase());

        // Mensaje genérico por seguridad (no revela si el email existe)
        String genericMessage = "Si el email existe, recibirá instrucciones de recuperación";

        if (user == null) {
            return genericMessage;
        }

        // Generar token único válido por 30 minutos
        String token = UUID.randomUUID().toString();
        Date expiration = new Date(System.currentTimeMillis() + 1800000); // 30 min

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(token);
        resetToken.setExpiration(expiration);
        resetToken.setUsed(false);
        resetToken.setUser(user);
        this.theTokenRepository.save(resetToken);

        // Enviar email con enlace de recuperación
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            String resetLink = "http://localhost:4200/reset-password?token=" + token;

            Map<String, String> emailBody = new HashMap<>();
            emailBody.put("to", user.getEmail());
            emailBody.put("subject", "Recuperación de contraseña");
            emailBody.put("body",
                    "<h1>Recuperación de contraseña</h1>" +
                            "<p>Hola " + user.getName() + ", recibimos una solicitud para restablecer tu contraseña.</p>" +
                            "<p>Haz clic en el siguiente enlace para continuar:</p>" +
                            "<a href='" + resetLink + "'>Restablecer contraseña</a>" +
                            "<p>Este enlace expira en 30 minutos.</p>" +
                            "<p>Si no solicitaste esto, ignora este mensaje.</p>"
            );

            HttpEntity<Map<String, String>> emailRequest = new HttpEntity<>(emailBody, headers);
            restTemplate.postForEntity("http://localhost:5000/send-email", emailRequest, String.class);
        } catch (Exception e) {
            System.out.println("Error enviando email de recuperación: " + e.getMessage());
        }

        return genericMessage;
    }

    // Restablecer contraseña con el token
    public String resetPassword(String token, String newPassword, String confirmPassword) {
        if (!newPassword.equals(confirmPassword)) {
            throw new RuntimeException("Las contraseñas no coinciden");
        }

        PasswordResetToken resetToken = this.theTokenRepository.findByToken(token);

        if (resetToken == null) {
            throw new RuntimeException("Token inválido");
        }
        if (resetToken.isUsed()) {
            throw new RuntimeException("El token ya fue utilizado");
        }
        if (resetToken.getExpiration().before(new Date())) {
            throw new RuntimeException("El token ha expirado");
        }

        // Validar requisitos de contraseña
        String passwordRegex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&._-])[A-Za-z\\d@$!%*?&._-]{8,}$";
        if (!newPassword.matches(passwordRegex)) {
            throw new RuntimeException("La contraseña no cumple los requisitos de seguridad");
        }

        // Actualizar contraseña
        User user = resetToken.getUser();
        user.setPassword(new EncryptionService().convertSHA256(newPassword));
        theUserRepository.save(user);

        // Marcar token como usado
        resetToken.setUsed(true);
        theTokenRepository.save(resetToken);

        return "Contraseña actualizada correctamente";
    }
}