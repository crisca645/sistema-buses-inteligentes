package com.ccrr.ms_security.Services;

import com.ccrr.ms_security.DTOs.AuthResponse;
import com.ccrr.ms_security.DTOs.GithubUserDto;
import com.ccrr.ms_security.Models.User;
import com.ccrr.ms_security.Repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

        User user = userRepository
                .getUserByAuthProviderAndProviderId("GITHUB", githubUser.getId());

        boolean isNewUser = false;

        if (user == null) {
            user = userRepository.getUserByEmail(githubUser.getEmail());
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
            user.setUsername(githubUser.getLogin()); // ← username de GitHub
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