package com.ccrr.ms_security.Services;

import com.ccrr.ms_security.Models.Permission;
import com.ccrr.ms_security.Models.Role;
import com.ccrr.ms_security.Models.RolePermission;
import com.ccrr.ms_security.Repositories.PermissionRepository;
import com.ccrr.ms_security.Repositories.RolePermissionRepository;
import com.ccrr.ms_security.Repositories.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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
        if (!StringUtils.hasText(roleId)) {
            return null;
        }

        roleId = roleId.trim();

        Role role = this.theRoleRepository.findById(roleId).orElse(null);
        if (role == null) {
            return null;
        }

        return this.theRolePermissionRepository.getPermissionsByRole(roleId);
    }

    public AddRolePermissionResult addRolePermission(String roleId, String permissionId) {
        if (!StringUtils.hasText(roleId) || !StringUtils.hasText(permissionId)) {
            return AddRolePermissionResult.NOT_FOUND;
        }

        roleId = roleId.trim();
        permissionId = permissionId.trim();

        Role role = this.theRoleRepository.findById(roleId).orElse(null);
        Permission permission = this.thePermissionRepository.findById(permissionId).orElse(null);

        if (role == null || permission == null) {
            return AddRolePermissionResult.NOT_FOUND;
        }

        List<RolePermission> existingRolePermissions =
                this.theRolePermissionRepository.getRolePermissions(roleId, permissionId);

        if (existingRolePermissions != null && !existingRolePermissions.isEmpty()) {
            return AddRolePermissionResult.DUPLICATE;
        }

        this.theRolePermissionRepository.save(new RolePermission(role, permission));
        return AddRolePermissionResult.SUCCESS;
    }

    public boolean removeRolePermission(String rolePermissionId) {
        if (!StringUtils.hasText(rolePermissionId)) {
            return false;
        }

        rolePermissionId = rolePermissionId.trim();

        RolePermission rolePermission =
                this.theRolePermissionRepository.findById(rolePermissionId).orElse(null);

        if (rolePermission == null) {
            return false;
        }

        this.theRolePermissionRepository.delete(rolePermission);
        return true;
    }
}