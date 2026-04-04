package com.ccrr.ms_security.Services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.Map;

@Service
public class RecaptchaService {

    @Value("${recaptcha.secret}")
    private String secretKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String VERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";
    private static final double MIN_SCORE = 0.5;

    public boolean validateToken(String token) {
        try {
            if (token == null || token.isBlank()) {
                System.out.println("reCAPTCHA: token vacío");
                return false;
            }

            String url = VERIFY_URL + "?secret=" + secretKey + "&response=" + token;
            Map response = restTemplate.postForObject(url, null, Map.class);

            System.out.println("reCAPTCHA response: " + response);

            if (response == null) return false;

            boolean success = Boolean.TRUE.equals(response.get("success"));
            double score = response.get("score") != null
                    ? ((Number) response.get("score")).doubleValue()
                    : 1.0;

            System.out.println("reCAPTCHA success: " + success + ", score: " + score);

            return success && score >= MIN_SCORE;

        } catch (Exception e) {
            System.out.println("reCAPTCHA error: " + e.getMessage());
            return false;
        }
    }
}