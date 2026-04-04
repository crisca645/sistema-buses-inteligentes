package com.ccrr.ms_security.DTOs;

import lombok.Data;

@Data
public class LinkProviderRequest {
    private String accessToken;  // para GitHub
    private String idToken;      // para Google y Microsoft
    private String provider;     // "GITHUB", "GOOGLE", "MICROSOFT"
}