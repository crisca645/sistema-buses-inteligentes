package com.example.ms_security.Services;

import com.example.ms_security.Models.Permission;
import com.example.ms_security.Models.Role;
import com.example.ms_security.Models.RolePermission;
import com.example.ms_security.Repositories.PermissionRepository;
import com.example.ms_security.Repositories.RolePermissionRepository;
import com.example.ms_security.Repositories.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RolePermissionService {

    public enum AddRolePermissionResult {
        SUCCESS,
        NOT_FOUND,
        DUPLICATE
    }

    @Autowired
    private RoleRepository theRoleRepository;

    @Autowired
    private PermissionRepository thePermissionRepository;

    @Autowired
    private RolePermissionRepository theRolePermissionRepository;

    public List<RolePermission> getPermissionsByRole(String roleId) {
        Role role = this.theRoleRepository.findById(roleId).orElse(null);

        if (role == null) {
            return null;
        }

        return this.theRolePermissionRepository.getPermissionsByRole(roleId);
    }

    public AddRolePermissionResult addRolePermission(String roleId, String permissionId) {
        Role role = this.theRoleRepository.findById(roleId).orElse(null);
        Permission permission = this.thePermissionRepository.findById(permissionId).orElse(null);

        if (role == null || permission == null) {
            return AddRolePermissionResult.NOT_FOUND;
        }

        // CAMBIO:
        // En vez de asumir que existe máximo una relación, consultamos una lista.
        // Si ya hay una o varias, devolvemos DUPLICATE y evitamos el 500.
        List<RolePermission> existingRolePermissions =
                this.theRolePermissionRepository.getRolePermissions(roleId, permissionId);

        if (existingRolePermissions != null && !existingRolePermissions.isEmpty()) {
            return AddRolePermissionResult.DUPLICATE;
        }

        RolePermission theRolePermission = new RolePermission(role, permission);
        this.theRolePermissionRepository.save(theRolePermission);

        return AddRolePermissionResult.SUCCESS;
    }

    public boolean removeRolePermission(String rolePermissionId) {
        RolePermission rolePermission = this.theRolePermissionRepository.findById(rolePermissionId).orElse(null);

        if (rolePermission != null) {
            this.theRolePermissionRepository.delete(rolePermission);
            return true;
        } else {
            return false;
        }
    }
}