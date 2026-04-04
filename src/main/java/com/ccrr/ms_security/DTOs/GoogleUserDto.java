package com.ccrr.ms_security.DTOs;

import lombok.Data;

@Data
public class GoogleUserDto {

    private String sub;
    private String email;
    private String name;
    private String picture;
    private Boolean emailVerified;

}