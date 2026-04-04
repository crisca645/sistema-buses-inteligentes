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

    public AuthResponse loginWithMicrosoft(String idToken) {

        MicrosoftUserDto microsoftUser = microsoftTokenVerifierService.verify(idToken);

        if (microsoftUser == null) {
            return null;
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
            user.setPicture(null);
            user.setEmailVerified(true);
            user.setActive(true);

            user = userRepository.save(user);
            isNewUser = true;
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