package com.ccrr.ms_security.Services;

import com.ccrr.ms_security.Models.Role;
import com.ccrr.ms_security.Models.User;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class EmailNotificationService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:}")
    private String fromEmail;

    public EmailNotificationService(ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.mailSender = mailSenderProvider.getIfAvailable();
    }

    public boolean sendRoleChangeNotification(User user, List<Role> currentRoles, String actionDescription) {
        if (user == null || !StringUtils.hasText(user.getEmail())) {
            return false;
        }

        if (this.mailSender == null || !StringUtils.hasText(this.fromEmail)) {
            return false;
        }

        try {
            String rolesText;
            if (currentRoles == null || currentRoles.isEmpty()) {
                rolesText = "Actualmente no tienes roles asignados.";
            } else {
                rolesText = currentRoles.stream()
                        .map(Role::getName)
                        .collect(Collectors.joining(", "));
                rolesText = "Tus roles actuales son: " + rolesText + ".";
            }

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(this.fromEmail);
            message.setTo(user.getEmail());
            message.setSubject("Actualización de roles y permisos");
            message.setText(
                    "Hola " + user.getName() + ",\n\n" +
                            "Se ha realizado un cambio en tus roles del sistema.\n" +
                            "Cambio realizado: " + actionDescription + ".\n\n" +
                            rolesText + "\n\n" +
                            "Tus permisos dentro de la plataforma pueden haber cambiado en función de estos roles.\n\n" +
                            "Este es un mensaje automático del sistema."
            );

            this.mailSender.send(message);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public boolean sendTwoFactorCodeNotification(User user, String verificationCode) {
        if (user == null || !StringUtils.hasText(user.getEmail()) || !StringUtils.hasText(verificationCode)) {
            return false;
        }

        if (this.mailSender == null || !StringUtils.hasText(this.fromEmail)) {
            return false;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(this.fromEmail);
            message.setTo(user.getEmail());
            message.setSubject("Código de verificación de acceso");
            message.setText(
                    "Hola " + user.getName() + ",\n\n" +
                            "Tu código de verificación para continuar el inicio de sesión es: " + verificationCode + ".\n\n" +
                            "Si no reconoces este intento de acceso, ignora este mensaje."
            );

            this.mailSender.send(message);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}