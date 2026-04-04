package com.ccrr.ms_security.Controllers;

import com.ccrr.ms_security.DTOs.*;
import com.ccrr.ms_security.Models.User;
import com.ccrr.ms_security.Repositories.UserRepository;
import com.ccrr.ms_security.Services.GoogleAuthService;
import com.ccrr.ms_security.Services.JwtService;
import com.ccrr.ms_security.Services.SecurityService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.ccrr.ms_security.Services.GithubAuthService;
import com.ccrr.ms_security.Services.MicrosoftAuthService;
import com.ccrr.ms_security.Services.GithubTokenVerifierService;
import com.ccrr.ms_security.Services.GoogleTokenVerifierService;
import com.ccrr.ms_security.Services.MicrosoftTokenVerifierService;
import com.ccrr.ms_security.DTOs.LoginRequest;
import com.ccrr.ms_security.Services.RecaptchaService;

import java.io.IOException;
import java.util.HashMap;

@CrossOrigin
@RestController
@RequestMapping("/security")
public class SecurityController {

    @Autowired
    private SecurityService theSecurityService;

    @Autowired
    private GoogleAuthService googleAuthService;

    @Autowired
    private GithubAuthService githubAuthService;

    @Autowired
    private MicrosoftAuthService microsoftAuthService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GithubTokenVerifierService githubTokenVerifierService;

    @Autowired
    private GoogleTokenVerifierService googleTokenVerifierService;

    @Autowired
    private MicrosoftTokenVerifierService microsoftTokenVerifierService;

    @Autowired
    private RecaptchaService recaptchaService;

    @PostMapping("login")
    public HashMap<String, Object> login(@RequestBody LoginRequest request,
                                         final HttpServletResponse response) throws IOException {
        HashMap<String, Object> theResponse = new HashMap<>();

        // Validar reCAPTCHA
        if (!recaptchaService.validateToken(request.getRecaptchaToken())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Verificación reCAPTCHA fallida");
            return theResponse;
        }

        User theNewUser = new User();
        theNewUser.setEmail(request.getEmail());
        theNewUser.setPassword(request.getPassword());

        String token = this.theSecurityService.login(theNewUser);

        if (token != null) {
            theResponse.put("token", token);
        } else {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return theResponse;
        }

        return theResponse;
    }

    @PostMapping("google")
    public HashMap<String, Object> googleLogin(@RequestBody GoogleLoginRequest request,
                                               final HttpServletResponse response) throws IOException {

        AuthResponse authResponse = googleAuthService.loginWithGoogle(request.getCredential());

        if (authResponse == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token de Google inválido");
            return new HashMap<>();
        }

        HashMap<String, Object> userResponse = new HashMap<>();
        userResponse.put("id", authResponse.getUser().getId());
        userResponse.put("name", authResponse.getUser().getName());
        userResponse.put("email", authResponse.getUser().getEmail());
        userResponse.put("authProvider", authResponse.getUser().getAuthProvider());
        userResponse.put("providerId", authResponse.getUser().getProviderId());
        userResponse.put("picture", authResponse.getUser().getPicture());
        userResponse.put("emailVerified", authResponse.getUser().getEmailVerified());
        userResponse.put("active", authResponse.getUser().getActive());

        HashMap<String, Object> theResponse = new HashMap<>();
        theResponse.put("token", authResponse.getToken());
        theResponse.put("user", userResponse);
        theResponse.put("isNewUser", authResponse.isNewUser());
        theResponse.put("requiresAdditionalInfo", authResponse.isRequiresAdditionalInfo());

        return theResponse;
    }

    @PostMapping("github")
    public HashMap<String, Object> githubLogin(@RequestBody GithubLoginRequest request,
                                               final HttpServletResponse response) throws IOException {

        AuthResponse authResponse = githubAuthService.loginWithGithub(request.getAccessToken());

        if (authResponse == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token GitHub inválido");
            return new HashMap<>();
        }

        HashMap<String, Object> res = new HashMap<>();
        res.put("token", authResponse.getToken());
        res.put("user", authResponse.getUser());
        res.put("isNewUser", authResponse.isNewUser());

        return res;
    }

    @PostMapping("microsoft")
    public HashMap<String, Object> microsoftLogin(@RequestBody MicrosoftLoginRequest request,
                                                  final HttpServletResponse response) throws IOException {

        AuthResponse authResponse = microsoftAuthService.loginWithMicrosoft(request.getIdToken());

        if (authResponse == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token Microsoft inválido");
            return new HashMap<>();
        }

        HashMap<String, Object> res = new HashMap<>();
        res.put("token", authResponse.getToken());
        res.put("user", authResponse.getUser());
        res.put("isNewUser", authResponse.isNewUser());

        return res;
    }

    @DeleteMapping("unlink/{provider}")
    public HashMap<String, Object> unlinkProvider(
            @PathVariable String provider,
            @RequestHeader("Authorization") String authHeader,
            final HttpServletResponse response) throws IOException {

        String jwt = authHeader.replace("Bearer ", "");
        User tokenUser = jwtService.getUserFromToken(jwt);

        if (tokenUser == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token inválido");
            return new HashMap<>();
        }

        String userId = tokenUser.getId();

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Usuario no encontrado");
            return new HashMap<>();
        }

        user.setAuthProvider(null);
        user.setProviderId(null);
        user.setUsername(null);
        userRepository.save(user);

        HashMap<String, Object> res = new HashMap<>();
        res.put("message", "Cuenta " + provider.toUpperCase() + " desvinculada correctamente");
        res.put("userId", userId);

        return res;
    }

    @PutMapping("complete-profile")
    public HashMap<String, Object> completeProfile(
            @RequestBody CompleteProfileRequest request,
            @RequestHeader("Authorization") String authHeader,
            final HttpServletResponse response) throws IOException {

        String jwt = authHeader.replace("Bearer ", "");
        User tokenUser = jwtService.getUserFromToken(jwt);

        if (tokenUser == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token inválido");
            return new HashMap<>();
        }

        User user = userRepository.findById(tokenUser.getId()).orElse(null);
        if (user == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Usuario no encontrado");
            return new HashMap<>();
        }

        if (request.getName() != null && !request.getName().isBlank()) {
            user.setName(request.getName());
        }
        if (request.getAddress() != null && !request.getAddress().isBlank()) {
            user.setAddress(request.getAddress());
        }
        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            user.setPhone(request.getPhone());
        }

        userRepository.save(user);

        HashMap<String, Object> res = new HashMap<>();
        res.put("message", "Perfil completado correctamente");
        res.put("user", user);
        return res;
    }

    @PostMapping("link/{provider}")
    public HashMap<String, Object> linkProvider(
            @PathVariable String provider,
            @RequestBody LinkProviderRequest request,
            @RequestHeader("Authorization") String authHeader,
            final HttpServletResponse response) throws IOException {

        String jwt = authHeader.replace("Bearer ", "");
        User tokenUser = jwtService.getUserFromToken(jwt);

        if (tokenUser == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token inválido");
            return new HashMap<>();
        }

        User user = userRepository.findById(tokenUser.getId()).orElse(null);
        if (user == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Usuario no encontrado");
            return new HashMap<>();
        }

        switch (provider.toUpperCase()) {

            case "GITHUB" -> {
                GithubUserDto githubUser = githubTokenVerifierService.verify(request.getAccessToken());
                if (githubUser == null) {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token GitHub inválido");
                    return new HashMap<>();
                }
                user.setAuthProvider("GITHUB");
                user.setProviderId(githubUser.getId());
                user.setUsername(githubUser.getLogin());
                if (user.getPicture() == null) user.setPicture(githubUser.getAvatarUrl());
            }

            case "GOOGLE" -> {
                GoogleUserDto googleUser = googleTokenVerifierService.verify(request.getIdToken());
                if (googleUser == null) {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token Google inválido");
                    return new HashMap<>();
                }
                user.setAuthProvider("GOOGLE");
                user.setProviderId(googleUser.getSub());
                if (user.getPicture() == null) user.setPicture(googleUser.getPicture());
            }

            case "MICROSOFT" -> {
                MicrosoftUserDto microsoftUser = microsoftTokenVerifierService.verify(request.getIdToken());
                if (microsoftUser == null) {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token Microsoft inválido");
                    return new HashMap<>();
                }
                user.setAuthProvider("MICROSOFT");
                user.setProviderId(microsoftUser.getSub());
            }

            default -> {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Proveedor no soportado");
                return new HashMap<>();
            }
        }

        userRepository.save(user);

        HashMap<String, Object> res = new HashMap<>();
        res.put("message", "Cuenta " + provider.toUpperCase() + " vinculada correctamente");
        res.put("user", user);
        return res;
    }
}