package com.ccrr.ms_security.DTOs;

import lombok.Data;

@Data
public class MicrosoftLoginRequest {
    private String idToken;
    private String accessToken;
}