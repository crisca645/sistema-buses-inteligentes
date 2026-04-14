package com.ccrr.ms_security.DTOs;

import lombok.Data;

@Data
public class MicrosoftUserDto {
    private String sub;
    private String email;
    private String name;
    private String picture;
}