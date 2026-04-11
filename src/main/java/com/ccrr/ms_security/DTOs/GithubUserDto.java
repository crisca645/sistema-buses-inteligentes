package com.ccrr.ms_security.DTOs;

import lombok.Data;

@Data
public class GithubUserDto {
    private String id;
    private String login;
    private String name;
    private String email;
    private String avatarUrl;
    private Boolean emailVerified;
    private Boolean emailRequired;
}