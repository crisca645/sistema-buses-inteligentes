package com.ccrr.ms_security.DTOs;

import lombok.Data;

@Data
public class GithubAlternateEmailRequest {
    private String providerId;
    private String username;
    private String name;
    private String picture;
    private String email;
}
