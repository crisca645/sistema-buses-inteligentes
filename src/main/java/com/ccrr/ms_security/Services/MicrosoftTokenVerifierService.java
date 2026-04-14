package com.ccrr.ms_security.Services;

import com.ccrr.ms_security.DTOs.MicrosoftUserDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
public class MicrosoftTokenVerifierService {

    @Value("${microsoft.client-id}")
    private String microsoftClientId;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public MicrosoftUserDto verify(String idToken) {
        try {
            if (idToken == null || idToken.isBlank()) {
                return null;
            }

            String[] parts = idToken.split("\\.");
            if (parts.length < 2) {
                return null;
            }

            String payloadJson = new String(
                    Base64.getUrlDecoder().decode(parts[1]),
                    StandardCharsets.UTF_8
            );

            Map<String, Object> payload = objectMapper.readValue(payloadJson, Map.class);

            // 1. Validar exp
            Object expObj = payload.get("exp");
            if (expObj == null) {
                return null;
            }

            long exp = ((Number) expObj).longValue();
            long now = Instant.now().getEpochSecond();

            if (exp < now) {
                return null;
            }

            // 2. Validar audience
            Object audObj = payload.get("aud");
            boolean audienceValid = false;

            if (audObj instanceof String aud) {
                audienceValid = microsoftClientId.equals(aud);
            } else if (audObj instanceof List<?> audList) {
                audienceValid = audList.contains(microsoftClientId);
            }

            if (!audienceValid) {
                return null;
            }

            // 3. Validar issuer
            String iss = payload.get("iss") != null ? payload.get("iss").toString() : null;
            if (iss == null || (
                    !iss.startsWith("https://login.microsoftonline.com/")
                            && !iss.startsWith("https://sts.windows.net/")
            )) {
                return null;
            }

            // 4. Extraer datos
            String sub = payload.get("sub") != null ? payload.get("sub").toString() : null;

            String email = null;
            if (payload.get("preferred_username") != null && !payload.get("preferred_username").toString().isBlank()) {
                email = payload.get("preferred_username").toString();
            } else if (payload.get("email") != null && !payload.get("email").toString().isBlank()) {
                email = payload.get("email").toString();
            } else if (payload.get("upn") != null && !payload.get("upn").toString().isBlank()) {
                email = payload.get("upn").toString();
            }

            String name = payload.get("name") != null ? payload.get("name").toString() : null;

            if (sub == null || sub.isBlank() || email == null || email.isBlank()) {
                return null;
            }

            MicrosoftUserDto microsoftUser = new MicrosoftUserDto();
            microsoftUser.setSub(sub);
            microsoftUser.setEmail(email);
            microsoftUser.setName(name);

            return microsoftUser;

        } catch (Exception e) {
            return null;
        }
    }
}