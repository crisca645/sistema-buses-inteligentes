package com.ccrr.ms_security.Services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class RecaptchaService {

    @Value("${recaptcha.secret}")
    private String secretKey;

    @Value("${recaptcha.score-threshold:0.5}")
    private Double scoreThreshold;

    private static final String VERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";

    public boolean verify(String token) {
        return verify(token, null);
    }

    public boolean verify(String token, String expectedAction) {
        if (token == null || token.isBlank()) {
            return false;
        }

        try {
            RestTemplate restTemplate = new RestTemplate();

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("secret", secretKey);
            params.add("response", token);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<MultiValueMap<String, String>> request =
                    new HttpEntity<>(params, headers);

            ResponseEntity<Map> response =
                    restTemplate.postForEntity(VERIFY_URL, request, Map.class);

            Map<?, ?> body = response.getBody();

            if (body == null) {
                return false;
            }

            Object successObject = body.get("success");
            if (!(successObject instanceof Boolean success) || !success) {
                return false;
            }

            Object scoreObject = body.get("score");
            if (scoreObject instanceof Number number) {
                double score = number.doubleValue();
                if (score < scoreThreshold) {
                    return false;
                }
            }

            if (expectedAction != null) {
                Object actionObject = body.get("action");
                if (!(actionObject instanceof String action) || !expectedAction.equals(action)) {
                    return false;
                }
            }

            return true;

        } catch (Exception e) {
            return false;
        }
    }
}