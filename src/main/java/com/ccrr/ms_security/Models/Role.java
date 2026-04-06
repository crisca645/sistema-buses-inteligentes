package com.ccrr.ms_security.Models;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document
public class Role {
    @Id
    private String id;
    private String name;
    private String description;

    public Role(String description, String name) {
        this.description = description;
        this.name = name;
    }
}
