package com.ccrr.ms_security.Models;

import lombok.Data;

@Data
public class RegisterRequest {

    private String name;
    private String lastname;
    private String email;
    private String password;
    private String confirmPassword;

}
