package com.ccrr.ms_security.Services;

import com.ccrr.ms_security.DTOs.AuthResponse;
import com.ccrr.ms_security.DTOs.GithubAlternateEmailRequest;
import com.ccrr.ms_security.DTOs.GithubUserDto;
import com.ccrr.ms_security.Models.User;
import com.ccrr.ms_security.Repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class GithubAuthService {

    @Autowired
    private GithubTokenVerifierService githubTokenVerifierService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private SecurityService securityService;

    public AuthResponse loginWithGithub(String accessToken) {

        GithubUserDto githubUser = githubTokenVerifierService.verify(accessToken);

        if (githubUser == null) return null;

        // CASO 1: GITHUB NO ENTREGA EMAIL
        if (Boolean.TRUE.equals(githubUser.getEmailRequired())) {
            User existing = userRepository
                    .getUserByAuthProviderAndProviderId("GITHUB", githubUser.getId());

            if (existing == null) {
                AuthResponse pending = new AuthResponse();
                pending.setRequiresEmailCompletion(true);
                pending.setCompletionMessage("Debe completar un email alternativo para continuar");
                pending.setPendingAuthProvider("GITHUB");
                pending.setPendingProviderId(githubUser.getId());
                pending.setPendingUsername(githubUser.getLogin());
                pending.setPendingName(displayNameFromGithub(githubUser));
                pending.setPendingPicture(githubUser.getAvatarUrl());
                return pending;
            }

            existing.setName(displayNameFromGithub(githubUser));
            existing.setPicture(githubUser.getAvatarUrl());
            existing.setUsername(githubUser.getLogin());
            existing.setEmailVerified(false);
            existing.setActive(false);
            existing = userRepository.save(existing);

            String token = jwtService.generateToken(existing);

            AuthResponse response = new AuthResponse();
            response.setToken(token);
            response.setUser(existing);
            response.setNewUser(existing.getEmail() == null);
            response.setRequiresAdditionalInfo(true);
            response.setEmailRequired(true);

            return response;
        }

        User user = userRepository
                .getUserByAuthProviderAndProviderId("GITHUB", githubUser.getId());

        boolean isNewUser = false;

        // CASO 2: YA EXISTE USUARIO GITHUB PERO TODAVÍA NO TIENE EMAIL COMPLETO
        if (user != null && (!StringUtils.hasText(user.getEmail()) || !Boolean.TRUE.equals(user.getActive()))) {
            user.setName(displayNameFromGithub(githubUser));
            user.setPicture(githubUser.getAvatarUrl());
            user.setUsername(githubUser.getLogin());
            user.setEmailVerified(false);
            user.setActive(false);
            user = userRepository.save(user);

            String token = jwtService.generateToken(user);

            AuthResponse response = new AuthResponse();
            response.setToken(token);
            response.setUser(user);
            response.setNewUser(false);
            response.setRequiresAdditionalInfo(true);
            response.setEmailRequired(true);

            return response;
        }

        if (user == null) {
            user = userRepository.getUserByEmail(githubUser.getEmail());

            // SI EXISTE POR EMAIL, SE VINCULA A GITHUB
            if (user != null) {
                user.setAuthProvider("GITHUB");
                user.setProviderId(githubUser.getId());
                user.setPicture(githubUser.getAvatarUrl());
                user.setUsername(githubUser.getLogin());
                user.setEmailVerified(true);
                user.setActive(true);

                if (!StringUtils.hasText(user.getName())) {
                    user.setName(displayNameFromGithub(githubUser));
                }

                user = userRepository.save(user);
            }
        }

        if (user == null) {
            user = new User();
            user.setName(displayNameFromGithub(githubUser));
            user.setEmail(githubUser.getEmail());
            user.setPassword(securityService.generateOAuthTechnicalPassword());
            user.setAuthProvider("GITHUB");
            user.setProviderId(githubUser.getId());
            user.setPicture(githubUser.getAvatarUrl());
            user.setEmailVerified(true);
            user.setActive(true);
            user.setUsername(githubUser.getLogin());
            user = userRepository.save(user);
            isNewUser = true;
        }

        String token = jwtService.generateToken(user);

        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setUser(user);
        response.setNewUser(isNewUser);
        response.setRequiresAdditionalInfo(isNewUser);
        response.setEmailRequired(false);

        return response;
    }

    public AuthResponse completeGithubEmail(User user, String email) {

        if (user == null) {
            throw new RuntimeException("Usuario no válido");
        }

        if (!"GITHUB".equals(user.getAuthProvider())) {
            throw new RuntimeException("El usuario no pertenece a autenticación con GitHub");
        }

        if (!StringUtils.hasText(email)) {
            throw new RuntimeException("El email es obligatorio");
        }

        String normalizedEmail = email.trim().toLowerCase();

        User existingUser = userRepository.getUserByEmail(normalizedEmail);
        if (existingUser != null && !existingUser.getId().equals(user.getId())) {
            throw new RuntimeException("El email ya está registrado por otro usuario");
        }

        user.setEmail(normalizedEmail);
        user.setEmailVerified(true);
        user.setActive(true);

        if (user.getName() == null || user.getName().isBlank()) {
            if (StringUtils.hasText(user.getUsername())) {
                user.setName(user.getUsername());
            } else {
                user.setName("Usuario GitHub");
            }
        }

        user = userRepository.save(user);

        String token = jwtService.generateToken(user);

        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setUser(user);
        response.setNewUser(false);
        response.setRequiresAdditionalInfo(false);
        response.setEmailRequired(false);

        return response;
    }

    /**
     * Registro tras OAuth GitHub sin email: crea usuario con email alternativo indicado por el usuario.
     */
    public AuthResponse registerGithubWithAlternateEmail(GithubAlternateEmailRequest req) {
        if (req == null || !StringUtils.hasText(req.getProviderId()) || !StringUtils.hasText(req.getEmail())) {
            throw new RuntimeException("providerId y email son obligatorios");
        }

        String providerId = req.getProviderId().trim();
        if (userRepository.getUserByAuthProviderAndProviderId("GITHUB", providerId) != null) {
            throw new RuntimeException("Esta cuenta GitHub ya está registrada; inicia sesión con GitHub");
        }

        String normalizedEmail = req.getEmail().trim().toLowerCase();
        if (!normalizedEmail.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) {
            throw new RuntimeException("El formato del email no es válido");
        }

        if (userRepository.getUserByEmail(normalizedEmail) != null) {
            throw new RuntimeException("El email ya está registrado por otro usuario");
        }

        User user = new User();
        user.setProviderId(providerId);
        user.setAuthProvider("GITHUB");
        user.setEmail(normalizedEmail);
        user.setPassword(securityService.generateOAuthTechnicalPassword());
        user.setPicture(StringUtils.hasText(req.getPicture()) ? req.getPicture().trim() : null);
        user.setUsername(StringUtils.hasText(req.getUsername()) ? req.getUsername().trim() : null);
        if (StringUtils.hasText(req.getName())) {
            user.setName(req.getName().trim());
        } else if (StringUtils.hasText(user.getUsername())) {
            user.setName(user.getUsername());
        } else {
            user.setName("Usuario GitHub");
        }
        user.setEmailVerified(true);
        user.setActive(true);

        user = userRepository.save(user);

        String token = jwtService.generateToken(user);
        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setUser(user);
        response.setNewUser(true);
        response.setRequiresAdditionalInfo(securityService.isProfileIncomplete(user));
        response.setEmailRequired(false);
        return response;
    }

    /**
     * Nombre para mostrar y persistir: API GitHub {@code name}, o si viene vacío el verificador ya usa {@code login}.
     */
    private String displayNameFromGithub(GithubUserDto githubUser) {
        if (githubUser == null) {
            return "Usuario GitHub";
        }
        if (StringUtils.hasText(githubUser.getName())) {
            return githubUser.getName().trim();
        }
        if (StringUtils.hasText(githubUser.getLogin())) {
            return githubUser.getLogin().trim();
        }
        return "Usuario GitHub";
    }
}