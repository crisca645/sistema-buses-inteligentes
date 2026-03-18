package com.ccrr.ms_security.Models;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document
public class User {
    //decodadores
    @Id
    private  String id;
    private  String name;
    private  String email;
    private  String password;

    //campos nuevos para el oggin con google

    private String authProvider;
    private String providerId;
    private String picture;
    private Boolean emailVerified;
    private Boolean active;


    public User() {
    }

    public User(String password, String email, String name) {
        this.password = password;
        this.email = email;
        this.name = name;
    }


}