package com.example.ms_security.Models;

import lombok.Data;

@Data
public class LoginResponse {
    private boolean requires2FA;
    private String sessionId;
    private String maskedEmail;
    private String message;

    public LoginResponse() {
    }

    public LoginResponse(boolean requires2FA, String sessionId, String maskedEmail, String message) {
        this.requires2FA = requires2FA;
        this.sessionId = sessionId;
        this.maskedEmail = maskedEmail;
        this.message = message;
    }
}