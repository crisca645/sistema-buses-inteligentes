package com.ccrr.ms_security.Services;

import com.ccrr.ms_security.DTOs.AuthResponse;
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

    public AuthResponse loginWithGithub(String accessToken) {

        GithubUserDto githubUser = githubTokenVerifierService.verify(accessToken);

        if (githubUser == null) return null;

        // CASO 1: GITHUB NO ENTREGA EMAIL
        if (Boolean.TRUE.equals(githubUser.getEmailRequired())) {
            User user = userRepository
                    .getUserByAuthProviderAndProviderId("GITHUB", githubUser.getId());

            if (user == null) {
                user = new User();
                user.setName(githubUser.getName());
                user.setEmail(null);
                user.setPassword("");
                user.setAuthProvider("GITHUB");
                user.setProviderId(githubUser.getId());
                user.setPicture(githubUser.getAvatarUrl());
                user.setEmailVerified(false);
                user.setActive(false);
                user.setUsername(githubUser.getLogin());
                user = userRepository.save(user);
            } else {
                user.setName(githubUser.getName());
                user.setPicture(githubUser.getAvatarUrl());
                user.setUsername(githubUser.getLogin());
                user.setEmailVerified(false);
                user.setActive(false);
                user = userRepository.save(user);
            }

            String token = jwtService.generateToken(user);

            AuthResponse response = new AuthResponse();
            response.setToken(token);
            response.setUser(user);
            response.setNewUser(user.getEmail() == null);
            response.setRequiresAdditionalInfo(true);
            response.setEmailRequired(true);

            return response;
        }

        User user = userRepository
                .getUserByAuthProviderAndProviderId("GITHUB", githubUser.getId());

        boolean isNewUser = false;

        // CASO 2: YA EXISTE USUARIO GITHUB PERO TODAVÍA NO TIENE EMAIL COMPLETO
        if (user != null && (!StringUtils.hasText(user.getEmail()) || !Boolean.TRUE.equals(user.getActive()))) {
            user.setName(githubUser.getName() != null ? githubUser.getName() : user.getName());
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

                if ((user.getName() == null || user.getName().isEmpty()) && githubUser.getName() != null) {
                    user.setName(githubUser.getName());
                }

                user = userRepository.save(user);
            }
        }

        if (user == null) {
            user = new User();
            user.setName(githubUser.getName());
            user.setEmail(githubUser.getEmail());
            user.setPassword("");
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
            user.setName("Usuario GitHub");
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
}