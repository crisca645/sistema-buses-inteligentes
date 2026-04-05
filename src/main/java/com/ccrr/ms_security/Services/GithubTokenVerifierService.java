package com.ccrr.ms_security.Services;

import com.ccrr.ms_security.DTOs.GithubUserDto;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class GithubTokenVerifierService {

    private final RestTemplate restTemplate = new RestTemplate();

    public GithubUserDto verify(String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.set("Accept", "application/vnd.github+json");

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            // 🔹 1. Obtener usuario
            ResponseEntity<Map> response = restTemplate.exchange(
                    "https://api.github.com/user",
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) return null;

            Map body = response.getBody();

            String email = (String) body.get("email");

            // 🔥 2. SI EMAIL ES NULL → IR A /user/emails
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

            // SI AÚN NO HAY EMAIL → RETORNAR DTO CON EMAIL REQUIRED
            if (email == null || email.isBlank()) {
                // 3. Construir DTO con emailRequired = true
                GithubUserDto dto = new GithubUserDto();
                dto.setId(String.valueOf(body.get("id")));
                dto.setLogin((String) body.get("login"));
                dto.setName((String) body.get("name"));
                dto.setEmail(null); // Email nulo
                dto.setAvatarUrl((String) body.get("avatar_url"));
                dto.setEmailVerified(false);
                dto.setEmailRequired(true);
                
                return dto;
            }

            // 3. Construir DTO
            GithubUserDto dto = new GithubUserDto();
            dto.setId(String.valueOf(body.get("id")));
            dto.setLogin((String) body.get("login"));
            dto.setName((String) body.get("name"));
            dto.setEmail(email);
            dto.setAvatarUrl((String) body.get("avatar_url"));
            dto.setEmailVerified(true);
            dto.setEmailRequired(false);

            return dto;

        } catch (Exception e) {
            return null;
        }
    }
}