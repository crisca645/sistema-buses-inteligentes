package com.ccrr.ms_security.Services;

import com.ccrr.ms_security.Models.Role;
import com.ccrr.ms_security.Models.RolePermission;
import com.ccrr.ms_security.Models.UserRole;
import com.ccrr.ms_security.Repositories.RolePermissionRepository;
import com.ccrr.ms_security.Repositories.RoleRepository;
import com.ccrr.ms_security.Repositories.UserRoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
public class RoleService {

    private static final int MIN_NAME_LENGTH = 3;
    private static final int MIN_DESCRIPTION_LENGTH = 5;

    public enum DeleteRoleResult {
        SUCCESS,
        NOT_FOUND,
        ROLE_ASSIGNED_TO_USERS
    }

    @Autowired
    private RoleRepository theRoleRepository;

    @Autowired
    private UserRoleRepository theUserRoleRepository;

    @Autowired
    private RolePermissionRepository theRolePermissionRepository;

    public List<Role> find() {
        return this.theRoleRepository.findAll();
    }

    public Role findById(String id) {
        return this.theRoleRepository.findById(id).orElse(null);
    }

    public Role create(Role newRole) {
        this.validateRequiredFieldsAndMinLength(newRole);
        newRole.setName(this.normalizeText(newRole.getName()));
        newRole.setDescription(this.normalizeText(newRole.getDescription()));
        this.assertNameUniqueAmongRoles(newRole.getName(), null);
        return this.theRoleRepository.save(newRole);
    }

    public Role update(String id, Role newRole) {
        Role actualRole = this.theRoleRepository.findById(id).orElse(null);
        if (actualRole == null) {
            return null;
        }

        this.validateRequiredFieldsAndMinLength(newRole);
        actualRole.setName(this.normalizeText(newRole.getName()));
        actualRole.setDescription(this.normalizeText(newRole.getDescription()));
        this.assertNameUniqueAmongRoles(actualRole.getName(), id);
        return this.theRoleRepository.save(actualRole);
    }

    public DeleteRoleResult delete(String id) {
        Role theRole = this.theRoleRepository.findById(id).orElse(null);
        if (theRole == null) {
            return DeleteRoleResult.NOT_FOUND;
        }

        List<UserRole> assignedUsers = this.theUserRoleRepository.getUsersByRole(id);
        if (assignedUsers != null && !assignedUsers.isEmpty()) {
            return DeleteRoleResult.ROLE_ASSIGNED_TO_USERS;
        }

        List<RolePermission> rolePermissions = this.theRolePermissionRepository.getPermissionsByRole(id);
        if (rolePermissions != null && !rolePermissions.isEmpty()) {
            this.theRolePermissionRepository.deleteAll(rolePermissions);
        }

        this.theRoleRepository.delete(theRole);
        return DeleteRoleResult.SUCCESS;
    }

    public void ensureDefaultRoles() {
        List<Role> existingRoles = new ArrayList<>(this.theRoleRepository.findAll());

        this.createDefaultRoleIfMissing(existingRoles,
                "Administrador Sistema",
                "Rol predeterminado de administración del sistema");
        this.createDefaultRoleIfMissing(existingRoles,
                "Administrador Empresa",
                "Rol predeterminado de administración de empresa");
        this.createDefaultRoleIfMissing(existingRoles,
                "Supervisor",
                "Rol predeterminado de supervisión operativa");
        this.createDefaultRoleIfMissing(existingRoles,
                "Conductor",
                "Rol predeterminado de conductores");
        this.createDefaultRoleIfMissing(existingRoles,
                "Ciudadano",
                "Rol predeterminado para ciudadanos del sistema");
    }

    private void createDefaultRoleIfMissing(List<Role> existingRoles, String name, String description) {
        boolean alreadyExists = existingRoles.stream()
                .anyMatch(role -> role != null && name.equalsIgnoreCase(role.getName()));

        if (!alreadyExists) {
            this.theRoleRepository.save(new Role(name, description));
        }
    }

    private void validateRequiredFieldsAndMinLength(Role role) {
        if (role == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role payload is required");
        }

        if (!StringUtils.hasText(role.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role name is required");
        }

        if (!StringUtils.hasText(role.getDescription())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role description is required");
        }

        String normalizedName = this.normalizeText(role.getName());
        String normalizedDescription = this.normalizeText(role.getDescription());

        if (normalizedName.length() < MIN_NAME_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Role name must have at least " + MIN_NAME_LENGTH + " characters");
        }

        if (normalizedDescription.length() < MIN_DESCRIPTION_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Role description must have at least " + MIN_DESCRIPTION_LENGTH + " characters");
        }
    }

    private String normalizeText(String value) {
        return value == null ? null : value.trim().replaceAll("\\s+", " ");
    }

    private void assertNameUniqueAmongRoles(String normalizedName, String excludeRoleId) {
        if (!StringUtils.hasText(normalizedName)) {
            return;
        }
        boolean duplicate = excludeRoleId == null
                ? this.theRoleRepository.existsByNameIgnoreCase(normalizedName)
                : this.theRoleRepository.existsByNameIgnoreCaseAndIdNot(normalizedName, excludeRoleId);
        if (duplicate) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe un rol con este nombre");
        }
    }
}