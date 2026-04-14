package com.ccrr.ms_security.Configurations;

import com.ccrr.ms_security.DTOs.AuthResponse;
import com.ccrr.ms_security.Models.User;
import com.ccrr.ms_security.Services.GithubAuthService;
import com.ccrr.ms_security.Services.SecurityService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Component
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    @Autowired
    private SecurityService securityService;

    @Autowired
    private GithubAuthService githubAuthService;

    @Autowired
    private OAuth2AuthorizedClientService authorizedClientService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
        String registrationId = token.getAuthorizedClientRegistrationId();

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        try {
            if ("github".equals(registrationId)) {
                handleGithubLogin(token, response);
                return;
            }

            handleGenericOAuthLogin(token, response);

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(
                    objectMapper.writeValueAsString(
                            Map.of("message", "Error en autenticación OAuth: " + e.getMessage())
                    )
            );
        }
    }

    private void handleGithubLogin(OAuth2AuthenticationToken token,
                                   HttpServletResponse response) throws IOException {

        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                token.getAuthorizedClientRegistrationId(),
                token.getName()
        );

        if (client == null || client.getAccessToken() == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(
                    objectMapper.writeValueAsString(
                            Map.of("message", "No se pudo obtener el access token de GitHub")
                    )
            );
            return;
        }

        String accessToken = client.getAccessToken().getTokenValue();
        AuthResponse authResponse = githubAuthService.loginWithGithub(accessToken);

        if (authResponse == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(
                    objectMapper.writeValueAsString(
                            Map.of("message", "No se pudo autenticar con GitHub")
                    )
            );
            return;
        }

        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write(objectMapper.writeValueAsString(authResponse));
    }

    private String getAccessToken(OAuth2AuthenticationToken token) {
        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                token.getAuthorizedClientRegistrationId(),
                token.getName()
        );

        if (client == null || client.getAccessToken() == null) {
            return null;
        }

        return client.getAccessToken().getTokenValue();
    }

    private Map<String, String> getMicrosoftGraphProfile(OAuth2AuthenticationToken token) {
        Map<String, String> profile = new HashMap<>();

        try {
            String accessToken = getAccessToken(token);
            if (accessToken == null) {
                return profile;
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            RestTemplate restTemplate = new RestTemplate();

            ResponseEntity<String> response = restTemplate.exchange(
                    "https://graph.microsoft.com/v1.0/me",
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return profile;
            }

            JsonNode node = objectMapper.readTree(response.getBody());

            profile.put("id", getText(node, "id"));
            profile.put("mail", getText(node, "mail"));
            profile.put("userPrincipalName", getText(node, "userPrincipalName"));
            profile.put("displayName", getText(node, "displayName"));
            profile.put("givenName", getText(node, "givenName"));
            profile.put("surname", getText(node, "surname"));

            return profile;

        } catch (Exception e) {
            return profile;
        }
    }

    private String getMicrosoftProfilePhoto(OAuth2AuthenticationToken token) {
        try {
            String accessToken = getAccessToken(token);
            if (accessToken == null) {
                return null;
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            RestTemplate restTemplate = new RestTemplate();

            ResponseEntity<byte[]> response = restTemplate.exchange(
                    "https://graph.microsoft.com/v1.0/me/photo/$value",
                    HttpMethod.GET,
                    entity,
                    byte[].class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                byte[] imageBytes = response.getBody();
                return Base64.getEncoder().encodeToString(imageBytes);
            }

            return null;

        } catch (HttpClientErrorException.NotFound e) {
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private String getText(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        if (value == null || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return text == null || text.isBlank() ? null : text;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }

        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }

    private String[] splitDisplayName(String displayName) {
        String[] result = new String[]{null, null};

        if (displayName == null || displayName.trim().isEmpty()) {
            return result;
        }

        String clean = displayName.trim();
        String[] parts = clean.split("\\s+", 2);

        result[0] = parts[0];
        if (parts.length > 1) {
            result[1] = parts[1];
        }

        return result;
    }

    private void handleGenericOAuthLogin(OAuth2AuthenticationToken token,
                                         HttpServletResponse response) throws IOException {

        OAuth2User oauthUser = token.getPrincipal();
        String registrationId = token.getAuthorizedClientRegistrationId();

        String email = null;
        String name = null;
        String lastname = null;
        String providerId = null;
        String picture = null;
        Boolean emailVerified = true;

        if ("google".equals(registrationId)) {
            email = oauthUser.getAttribute("email");
            name = oauthUser.getAttribute("given_name");
            lastname = oauthUser.getAttribute("family_name");
            providerId = oauthUser.getAttribute("sub");
            picture = oauthUser.getAttribute("picture");
            emailVerified = Boolean.TRUE.equals(oauthUser.getAttribute("email_verified"));
        }

        if ("microsoft".equals(registrationId)) {
            Map<String, String> graphProfile = getMicrosoftGraphProfile(token);

            String displayName = firstNonBlank(
                    graphProfile.get("displayName"),
                    oauthUser.getAttribute("name")
            );

            String[] splitName = splitDisplayName(displayName);

            email = firstNonBlank(
                    graphProfile.get("mail"),
                    graphProfile.get("userPrincipalName"),
                    oauthUser.getAttribute("email"),
                    oauthUser.getAttribute("preferred_username")
            );

            name = firstNonBlank(
                    graphProfile.get("givenName"),
                    splitName[0],
                    oauthUser.getAttribute("givenName"),
                    oauthUser.getAttribute("name")
            );

            lastname = firstNonBlank(
                    graphProfile.get("surname"),
                    splitName[1],
                    oauthUser.getAttribute("surname")
            );

            providerId = firstNonBlank(
                    graphProfile.get("id"),
                    oauthUser.getAttribute("sub")
            );

            picture = getMicrosoftProfilePhoto(token);
            emailVerified = true;
        }

        if (email == null || email.isBlank()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(
                    objectMapper.writeValueAsString(
                            Map.of("message", "No se pudo obtener el email. Verifica tu cuenta en " + registrationId)
                    )
            );
            return;
        }

        User user = securityService.findOrCreateOAuthUser(
                email,
                name,
                lastname,
                registrationId.toUpperCase(),
                providerId,
                picture,
                emailVerified
        );

        if (user == null) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(
                    objectMapper.writeValueAsString(
                            Map.of("message", "Error al crear o encontrar el usuario")
                    )
            );
            return;
        }

        String jwt = securityService.generateTokenForUser(user);

        String redirectUrl = "http://localhost:4200/oauth-success?token=" +
                URLEncoder.encode(jwt, StandardCharsets.UTF_8);

        response.sendRedirect(redirectUrl);
    }
}