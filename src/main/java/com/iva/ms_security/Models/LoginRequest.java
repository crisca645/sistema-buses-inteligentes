package com.iva.ms_security.Models;

import lombok.Data;

@Data
public class LoginRequest {
    private String email;
    private String password;
}
