package com.ccrr.ms_security.Configurations;

import com.ccrr.ms_security.Models.User;
import com.ccrr.ms_security.Repositories.UserRepository;
import com.ccrr.ms_security.Services.SecurityService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    @Autowired
    private SecurityService securityService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
        OAuth2User oauthUser = token.getPrincipal();
        String registrationId = token.getAuthorizedClientRegistrationId(); // "google", "github", "microsoft"

        String email = null;
        String name = null;
        String lastname = null;

        if ("google".equals(registrationId)) {
            email = oauthUser.getAttribute("email");
            name = oauthUser.getAttribute("given_name");
            lastname = oauthUser.getAttribute("family_name");

        } else if ("github".equals(registrationId)) {
            email = oauthUser.getAttribute("email");
            String fullName = oauthUser.getAttribute("name");
            if (fullName != null && fullName.contains(" ")) {
                name = fullName.substring(0, fullName.indexOf(" "));
                lastname = fullName.substring(fullName.indexOf(" ") + 1);
            } else {
                name = fullName != null ? fullName : oauthUser.getAttribute("login");
                lastname = "GitHub";
            }

        } else if ("microsoft".equals(registrationId)) {
            email = oauthUser.getAttribute("email");
            if (email == null) email = oauthUser.getAttribute("preferred_username");
            name = oauthUser.getAttribute("givenName");
            lastname = oauthUser.getAttribute("surname");
        }

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        if (email == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(
                    new ObjectMapper().writeValueAsString(
                            Map.of("message", "No se pudo obtener el email. Verifica que tu email sea público en " + registrationId)
                    )
            );
            return;
        }

        User user = securityService.findOrCreateGoogleUser(email, name, lastname);

        if (user == null) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(
                    new ObjectMapper().writeValueAsString(
                            Map.of("message", "Error al crear o encontrar el usuario")
                    )
            );
            return;
        }

        String jwt = securityService.generateTokenForUser(user);

        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write(
                new ObjectMapper().writeValueAsString(
                        Map.of(
                                "message", "Inicio de sesión con " + registrationId + " exitoso",
                                "token", jwt
                        )
                )
        );
    }
}