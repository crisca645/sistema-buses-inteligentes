package com.ccrr.ms_security.Services;

import com.ccrr.ms_security.DTOs.AuthResponse;
import com.ccrr.ms_security.DTOs.GoogleUserDto;
import com.ccrr.ms_security.Models.User;
import com.ccrr.ms_security.Repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GoogleAuthService {

    @Autowired
    private GoogleTokenVerifierService googleTokenVerifierService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    public AuthResponse loginWithGoogle(String credential) {

        // 1️⃣ Verificar token de Google
        GoogleUserDto googleUser = googleTokenVerifierService.verify(credential);

        if (googleUser == null) {
            return null;
        }

        // 2️⃣ Buscar usuario por providerId
        User user = userRepository
                .getUserByAuthProviderAndProviderId("GOOGLE", googleUser.getSub());

        boolean isNewUser = false;

        // 3️⃣ Si no existe, buscar por email
        if (user == null) {
            user = userRepository.getUserByEmail(googleUser.getEmail());
        }

        // 4️⃣ Si tampoco existe, crear usuario
        if (user == null) {

            user = new User();
            user.setName(googleUser.getName());
            user.setEmail(googleUser.getEmail());
            user.setPassword("");
            user.setAuthProvider("GOOGLE");
            user.setProviderId(googleUser.getSub());
            user.setPicture(googleUser.getPicture());
            user.setEmailVerified(googleUser.getEmailVerified());
            user.setActive(true);

            user = userRepository.save(user);

            isNewUser = true;
        }

        // 5️⃣ Generar JWT propio del sistema
        String token = jwtService.generateToken(user);

        // 6️⃣ Crear respuesta
        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setUser(user);
        response.setNewUser(isNewUser);
        response.setRequiresAdditionalInfo(isNewUser);

        return response;
    }
}