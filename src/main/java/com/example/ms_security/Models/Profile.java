package com.example.ms_security.Models;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document
public class Profile {
    @Id
    private String id;
    private String nombre;
    private String phone;  // <-- minúscula

    @DBRef
    private User user;

    public Profile() {
    }

    public Profile(String nombre, String phone) {
        this.nombre = nombre;
        this.phone = phone;  // <-- minúscula
    }
}
