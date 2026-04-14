package com.ccrr.ms_security.Models;

import lombok.Data;

import java.util.List;

@Data
public class UserWithRolesResponse {

    private String id;
    private String name;
    private String lastname;
    private String email;
    private List<Role> roles;

    public UserWithRolesResponse() {
    }

    public UserWithRolesResponse(String id, String name, String lastname, String email, List<Role> roles) {
        this.id = id;
        this.name = name;
        this.lastname = lastname;
        this.email = email;
        this.roles = roles;
    }
}