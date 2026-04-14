package com.ccrr.ms_security.Services;

import com.ccrr.ms_security.DTOs.GithubUserDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class GithubTokenVerifierService {

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Solo desarrollo / pruebas HU: fuerza flujo “sin email” aunque GitHub devuelva correo.
     * Desactivar en producción: {@code app.github.force-email-null-for-testing=false}
     */
    @Value("${app.github.force-email-null-for-testing:false}")
    private boolean forceGithubEmailNullForTesting;

    public GithubUserDto verify(String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.set("Accept", "application/vnd.github+json");

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    "https://api.github.com/user",
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) return null;

            Map body = response.getBody();
            if (body == null) return null;

            String login = body.get("login") != null ? String.valueOf(body.get("login")) : null;
            String resolvedName = resolveGithubDisplayName(body, login);

            String email = (String) body.get("email");

            if (email == null || email.isBlank()) {

                ResponseEntity<Map[]> emailsResponse = restTemplate.exchange(
                        "https://api.github.com/user/emails",
                        HttpMethod.GET,
                        entity,
                        Map[].class
                );

                if (emailsResponse.getBody() != null) {
                    for (Map item : emailsResponse.getBody()) {

                        Boolean primary = item.get("primary") != null && (Boolean) item.get("primary");
                        Boolean verified = item.get("verified") != null && (Boolean) item.get("verified");

                        if (Boolean.TRUE.equals(primary) && Boolean.TRUE.equals(verified)) {
                            email = (String) item.get("email");
                            break;
                        }
                    }
                }
            }

            if (forceGithubEmailNullForTesting) {
                email = null;
            }

            if (email == null || email.isBlank()) {
                GithubUserDto dto = new GithubUserDto();
                dto.setId(String.valueOf(body.get("id")));
                dto.setLogin(login);
                dto.setName(resolvedName);
                dto.setEmail(null);
                dto.setAvatarUrl((String) body.get("avatar_url"));
                dto.setEmailVerified(false);
                dto.setEmailRequired(true);

                return dto;
            }

            GithubUserDto dto = new GithubUserDto();
            dto.setId(String.valueOf(body.get("id")));
            dto.setLogin(login);
            dto.setName(resolvedName);
            dto.setEmail(email);
            dto.setAvatarUrl((String) body.get("avatar_url"));
            dto.setEmailVerified(true);
            dto.setEmailRequired(false);

            return dto;

        } catch (Exception e) {
            return null;
        }
    }

    /**
     * GitHub puede devolver {@code name} nulo si el usuario no configuró nombre público.
     * Para persistir un nombre útil usamos: nombre API → si falta, {@code login}.
     */
    private String resolveGithubDisplayName(Map body, String login) {
        Object nameObj = body != null ? body.get("name") : null;
        String apiName = nameObj != null ? String.valueOf(nameObj) : null;
        if (StringUtils.hasText(apiName)) {
            return apiName.trim();
        }
        if (StringUtils.hasText(login)) {
            return login.trim();
        }
        return null;
    }
}
