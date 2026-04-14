package com.ccrr.ms_security.Models;

import lombok.Data;

@Data
public class UserRoleByUserResponse {

    private String userRoleId;
    private String roleId;
    private String roleName;
    private String roleDescription;

    public UserRoleByUserResponse() {
    }

    public UserRoleByUserResponse(String userRoleId, String roleId, String roleName, String roleDescription) {
        this.userRoleId = userRoleId;
        this.roleId = roleId;
        this.roleName = roleName;
        this.roleDescription = roleDescription;
    }
}