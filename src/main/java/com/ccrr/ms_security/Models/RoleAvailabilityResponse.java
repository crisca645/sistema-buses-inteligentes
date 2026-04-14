package com.ccrr.ms_security.Models;

import lombok.Data;

@Data
public class RoleAvailabilityResponse {

    private String id;
    private String name;
    private String description;
    private boolean assigned;

    public RoleAvailabilityResponse() {
    }

    public RoleAvailabilityResponse(String id, String name, String description, boolean assigned) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.assigned = assigned;
    }
}