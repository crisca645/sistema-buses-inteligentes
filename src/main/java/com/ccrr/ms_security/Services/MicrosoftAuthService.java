package com.ccrr.ms_security.Services;

import com.ccrr.ms_security.DTOs.AuthResponse;
import com.ccrr.ms_security.DTOs.MicrosoftUserDto;
import com.ccrr.ms_security.Models.User;
import com.ccrr.ms_security.Repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MicrosoftAuthService {

    @Autowired
    private MicrosoftTokenVerifierService microsoftTokenVerifierService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private MicrosoftGraphService microsoftGraphService;

    public AuthResponse loginWithMicrosoft(String idToken, String accessToken) {

        MicrosoftUserDto microsoftUser = microsoftTokenVerifierService.verify(idToken);

        if (microsoftUser == null) {
            return null;
        }

        // Obtener foto de perfil si se proporcionó accessToken
        if (accessToken != null && !accessToken.isBlank()) {
            try {
                String picture = microsoftGraphService.getProfilePicture(accessToken);
                microsoftUser.setPicture(picture);
            } catch (Exception e) {
                // Ignorar errores de la foto para no romper el flujo de login
                microsoftUser.setPicture(null);
            }
        }

        User user = userRepository
                .getUserByAuthProviderAndProviderId("MICROSOFT", microsoftUser.getSub());

        boolean isNewUser = false;

        if (user == null) {
            user = userRepository.getUserByEmail(microsoftUser.getEmail());
        }

        if (user == null) {
            user = new User();
            user.setName(microsoftUser.getName());
            user.setEmail(microsoftUser.getEmail());
            user.setPassword("");
            user.setAuthProvider("MICROSOFT");
            user.setProviderId(microsoftUser.getSub());
            user.setPicture(microsoftUser.getPicture());
            user.setEmailVerified(true);
            user.setActive(true);

            user = userRepository.save(user);
            isNewUser = true;
        } else {
            // Si el usuario ya existía y no tenía foto, actualizarla
            if (user.getPicture() == null && microsoftUser.getPicture() != null) {
                user.setPicture(microsoftUser.getPicture());
                user = userRepository.save(user);
            }
        }

        String token = jwtService.generateToken(user);

        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setUser(user);
        response.setNewUser(isNewUser);
        response.setRequiresAdditionalInfo(isNewUser);

        return response;
    }
}