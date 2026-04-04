package com.ccrr.ms_security.DTOs;

import lombok.Data;

@Data
public class LoginRequest {
    private String email;
    private String password;
    private String recaptchaToken;
}