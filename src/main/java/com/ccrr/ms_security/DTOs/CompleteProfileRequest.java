package com.ccrr.ms_security.DTOs;

import lombok.Data;

@Data
public class CompleteProfileRequest {
    private String address;
    private String name;
    private String phone;
}