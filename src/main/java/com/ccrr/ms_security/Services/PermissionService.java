package com.ccrr.ms_security.Services;

import com.ccrr.ms_security.Models.Permission;
import com.ccrr.ms_security.Repositories.PermissionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PermissionService {

    @Autowired
    private PermissionRepository thePermissionRepository;

    public List<Permission> find() {
        return this.thePermissionRepository.findAll();
    }

    public Permission findById(String id) {
        return this.thePermissionRepository.findById(id).orElse(null);
    }

    public Permission create(Permission newPermission) {
        return this.thePermissionRepository.save(newPermission);
    }

    public Permission update(String id, Permission newPermission) {
        Permission actualPermission = this.findById(id);
        if (actualPermission == null) {
            return null;
        }

        actualPermission.setUrl(newPermission.getUrl());
        actualPermission.setMethod(newPermission.getMethod());
        actualPermission.setModel(newPermission.getModel());
        return this.thePermissionRepository.save(actualPermission);
    }

    public void delete(String id) {
        this.thePermissionRepository.deleteById(id);
    }

    public void ensureDefaultPermissions() {
        // Core security administration
        this.createPermissionIfMissing("/users", "GET", "User");
        this.createPermissionIfMissing("/users/?", "GET", "User");
        this.createPermissionIfMissing("/users/?", "PUT", "User");
        this.createPermissionIfMissing("/users/?", "DELETE", "User");
        this.createPermissionIfMissing("/users/?/profile/?", "POST", "UserProfile");
        this.createPermissionIfMissing("/users/?/profile/?", "DELETE", "UserProfile");
        this.createPermissionIfMissing("/users/?/session/?", "POST", "UserSession");
        this.createPermissionIfMissing("/users/?/session/?", "DELETE", "UserSession");

        this.createPermissionIfMissing("/roles", "GET", "Role");
        this.createPermissionIfMissing("/roles", "POST", "Role");
        this.createPermissionIfMissing("/roles/?", "GET", "Role");
        this.createPermissionIfMissing("/roles/?", "PUT", "Role");
        this.createPermissionIfMissing("/roles/?", "DELETE", "Role");

        this.createPermissionIfMissing("/permissions", "GET", "Permission");
        this.createPermissionIfMissing("/permissions", "POST", "Permission");
        this.createPermissionIfMissing("/permissions/?", "GET", "Permission");
        this.createPermissionIfMissing("/permissions/?", "PUT", "Permission");
        this.createPermissionIfMissing("/permissions/?", "DELETE", "Permission");

        this.createPermissionIfMissing("/profiles", "GET", "Profile");
        this.createPermissionIfMissing("/profiles", "POST", "Profile");
        this.createPermissionIfMissing("/profiles/?", "GET", "Profile");
        this.createPermissionIfMissing("/profiles/?", "PUT", "Profile");
        this.createPermissionIfMissing("/profiles/?", "DELETE", "Profile");

        this.createPermissionIfMissing("/sessions", "GET", "Session");
        this.createPermissionIfMissing("/sessions", "POST", "Session");
        this.createPermissionIfMissing("/sessions/?", "GET", "Session");
        this.createPermissionIfMissing("/sessions/?", "PUT", "Session");
        this.createPermissionIfMissing("/sessions/?", "DELETE", "Session");

        this.createPermissionIfMissing("/user-role/users-with-roles", "GET", "UserRole");
        this.createPermissionIfMissing("/user-role/user/?/available-roles", "GET", "UserRole");
        this.createPermissionIfMissing("/user-role/user/?/roles", "GET", "UserRole");
        this.createPermissionIfMissing("/user-role/?/role/?", "POST", "UserRole");
        this.createPermissionIfMissing("/user-role/user/?/role/?", "POST", "UserRole");
        this.createPermissionIfMissing("/user-role/?", "DELETE", "UserRole");

        this.createPermissionIfMissing("/role-permission/role/?", "GET", "RolePermission");
        this.createPermissionIfMissing("/role-permission/?/permission/?", "POST", "RolePermission");
        this.createPermissionIfMissing("/role-permission/role/?/permission/?", "POST", "RolePermission");
        this.createPermissionIfMissing("/role-permission/?", "DELETE", "RolePermission");

        // Domain modules requested by HU-ENTR-1-001
        this.createPermissionIfMissing("/buses", "GET", "Bus");
        this.createPermissionIfMissing("/buses", "POST", "Bus");
        this.createPermissionIfMissing("/buses/?", "PUT", "Bus");
        this.createPermissionIfMissing("/buses/?", "DELETE", "Bus");

        this.createPermissionIfMissing("/routes", "GET", "Route");
        this.createPermissionIfMissing("/routes", "POST", "Route");
        this.createPermissionIfMissing("/routes/?", "PUT", "Route");
        this.createPermissionIfMissing("/routes/?", "DELETE", "Route");

        this.createPermissionIfMissing("/schedules", "GET", "Schedule");
        this.createPermissionIfMissing("/schedules", "POST", "Schedule");
        this.createPermissionIfMissing("/schedules/?", "PUT", "Schedule");
        this.createPermissionIfMissing("/schedules/?", "DELETE", "Schedule");

        this.createPermissionIfMissing("/reports", "GET", "Report");

        this.createPermissionIfMissing("/incidents", "GET", "Incident");
        this.createPermissionIfMissing("/incidents", "POST", "Incident");
        this.createPermissionIfMissing("/incidents/?", "PUT", "Incident");
        this.createPermissionIfMissing("/incidents/?", "DELETE", "Incident");

        this.createPermissionIfMissing("/mass-messages", "POST", "MassMessage");
    }

    private void createPermissionIfMissing(String url, String method, String model) {
        if (!this.thePermissionRepository.existsByUrlAndMethod(url, method)) {
            this.thePermissionRepository.save(new Permission(url, method, model));
        }
    }
}