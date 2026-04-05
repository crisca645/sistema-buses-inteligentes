package com.example.ms_security.Models;

import lombok.Data;

import java.util.List;

@Data
public class UserAvailableRolesResponse {

    private String id;
    private String name;
    private String email;
    private List<RoleAvailabilityResponse> availableRoles;

    public UserAvailableRolesResponse() {
    }

    public UserAvailableRolesResponse(String id, String name, String email, List<RoleAvailabilityResponse> availableRoles) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.availableRoles = availableRoles;
    }
}