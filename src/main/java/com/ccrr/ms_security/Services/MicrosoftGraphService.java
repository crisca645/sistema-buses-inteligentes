package com.ccrr.ms_security.Services;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;

@Service
public class MicrosoftGraphService {

    private final RestTemplate restTemplate = new RestTemplate();

    public String getProfilePicture(String accessToken) {
        try {
            if (accessToken == null || accessToken.isBlank()) {
                return null;
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.set("Accept", "image/jpeg");

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<byte[]> response = restTemplate.exchange(
                    "https://graph.microsoft.com/v1.0/me/photo/$value",
                    HttpMethod.GET,
                    entity,
                    byte[].class
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return null;
            }

            byte[] imageBytes = response.getBody();

            // Validar que la imagen no pese más de 1MB
            if (imageBytes.length > 1024 * 1024) {
                return null;
            }

            // Convertir a Base64 con prefijo
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
            return "data:image/jpeg;base64," + base64Image;

        } catch (Exception e) {
            // No lanzar excepción para no romper el flujo de login
            return null;
        }
    }
}
