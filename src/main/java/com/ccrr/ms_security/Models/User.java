package com.ccrr.ms_security.Models;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document
public class User {
    @Id
    private String id;
    private String name;
    private String lastname;
    private String email;
    private String password;

    // OAuth fields
    private String authProvider;
    private String providerId;
    private String picture;
    private Boolean emailVerified;
    private Boolean active;
    private String username;
    private String address;
    private String phone;

    public User() {
    }

    public User(String password, String email, String name) {
        this.password = password;
        this.email = email;
        this.name = name;
    }
}