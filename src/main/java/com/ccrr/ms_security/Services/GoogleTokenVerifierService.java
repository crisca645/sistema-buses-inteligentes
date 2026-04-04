package com.ccrr.ms_security.Services;

import com.ccrr.ms_security.DTOs.GoogleUserDto;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class GoogleTokenVerifierService {

    @Value("${google.client-id}")
    private String googleClientId;

    public GoogleUserDto verify(String credential) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance()
            ).setAudience(Collections.singletonList(googleClientId)).build();

            GoogleIdToken idToken = verifier.verify(credential);

            if (idToken == null) {
                return null;
            }

            GoogleIdToken.Payload payload = idToken.getPayload();

            GoogleUserDto googleUser = new GoogleUserDto();
            googleUser.setSub(payload.getSubject());
            googleUser.setEmail(payload.getEmail());
            googleUser.setName((String) payload.get("name"));
            googleUser.setPicture((String) payload.get("picture"));
            googleUser.setEmailVerified((Boolean) payload.getEmailVerified());

            return googleUser;

        } catch (Exception e) {
            return null;
        }
    }
}