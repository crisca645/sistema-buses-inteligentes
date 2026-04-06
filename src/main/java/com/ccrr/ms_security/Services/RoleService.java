package com.ccrr.ms_security.Services;

import com.ccrr.ms_security.Models.Role;
import com.ccrr.ms_security.Models.UserRole;
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

    // CAMBIO:
    // Se mantienen las reglas que ya validaste en criterio 1.
    private static final int MIN_NAME_LENGTH = 3;
    private static final int MIN_DESCRIPTION_LENGTH = 5;

    // CAMBIO:
    // Resultado del borrado para que el controlador responda correctamente.
    public enum DeleteRoleResult {
        SUCCESS,
        NOT_FOUND,
        ROLE_ASSIGNED_TO_USERS
    }

    @Autowired
    private RoleRepository theRoleRepository;

    // CAMBIO:
    // Se inyecta UserRoleRepository para validar si el rol está asignado.
    @Autowired
    private UserRoleRepository theUserRoleRepository;

    public List<Role> find() {
        return this.theRoleRepository.findAll();
    }

    public Role findById(String id) {
        return this.theRoleRepository.findById(id).orElse(null);
    }

    public Role create(Role newRole) {
        // CAMBIO:
        // Se mantienen validaciones previas.
        this.validateRequiredFieldsAndMinLength(newRole);

        // CAMBIO:
        // Se normalizan espacios antes de guardar.
        newRole.setName(this.normalizeText(newRole.getName()));
        newRole.setDescription(this.normalizeText(newRole.getDescription()));

        return this.theRoleRepository.save(newRole);
    }

    public Role update(String id, Role newRole) {
        Role actualRole = this.theRoleRepository.findById(id).orElse(null);

        if (actualRole != null) {
            // CAMBIO:
            // Se mantienen validaciones previas.
            this.validateRequiredFieldsAndMinLength(newRole);

            actualRole.setName(this.normalizeText(newRole.getName()));
            actualRole.setDescription(this.normalizeText(newRole.getDescription()));

            this.theRoleRepository.save(actualRole);
            return actualRole;
        } else {
            return null;
        }
    }

    public DeleteRoleResult delete(String id) {
        Role theRole = this.theRoleRepository.findById(id).orElse(null);

        // CAMBIO:
        // Si el rol no existe, se informa al controlador.
        if (theRole == null) {
            return DeleteRoleResult.NOT_FOUND;
        }

        // CAMBIO:
        // Antes de eliminar, se valida si el rol tiene usuarios asignados.
        List<UserRole> assignedUsers = this.theUserRoleRepository.getUsersByRole(id);
        if (assignedUsers != null && !assignedUsers.isEmpty()) {
            return DeleteRoleResult.ROLE_ASSIGNED_TO_USERS;
        }

        // CAMBIO:
        // Solo se elimina si no tiene usuarios asignados.
        this.theRoleRepository.delete(theRole);
        return DeleteRoleResult.SUCCESS;
    }

    // CAMBIO:
    // Se mantiene la carga de roles predeterminados del criterio 2.
    public void ensureDefaultRoles() {
        List<Role> existingRoles = new ArrayList<>(this.theRoleRepository.findAll());

        this.createDefaultRoleIfMissing(
                existingRoles,
                "Administrador Sistema",
                "Rol predeterminado de administración del sistema"
        );
        this.createDefaultRoleIfMissing(
                existingRoles,
                "Administrador Empresa",
                "Rol predeterminado de administración de empresa"
        );
        this.createDefaultRoleIfMissing(
                existingRoles,
                "Supervisor",
                "Rol predeterminado de supervisión"
        );
        this.createDefaultRoleIfMissing(
                existingRoles,
                "Conductor",
                "Rol predeterminado para conductores"
        );
        this.createDefaultRoleIfMissing(
                existingRoles,
                "Ciudadano",
                "Rol predeterminado para ciudadanos"
        );
    }

    private void createDefaultRoleIfMissing(List<Role> existingRoles, String roleName, String description) {
        if (!this.roleNameExists(existingRoles, roleName)) {
            Role defaultRole = new Role(roleName, description);
            Role savedRole = this.create(defaultRole);
            existingRoles.add(savedRole);
        }
    }

    private boolean roleNameExists(List<Role> existingRoles, String roleName) {
        String normalizedRoleName = this.normalizeText(roleName);

        for (Role role : existingRoles) {
            if (role != null && StringUtils.hasText(role.getName())) {
                String existingName = this.normalizeText(role.getName());
                if (existingName.equalsIgnoreCase(normalizedRoleName)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void validateRequiredFieldsAndMinLength(Role role) {
        if (role == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El rol es obligatorio");
        }

        if (!StringUtils.hasText(role.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre del rol es obligatorio");
        }

        if (!StringUtils.hasText(role.getDescription())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La descripción del rol es obligatoria");
        }

        String normalizedName = this.normalizeText(role.getName());
        String normalizedDescription = this.normalizeText(role.getDescription());

        if (normalizedName.length() < MIN_NAME_LENGTH) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El nombre del rol debe tener mínimo " + MIN_NAME_LENGTH + " caracteres"
            );
        }

        if (normalizedDescription.length() < MIN_DESCRIPTION_LENGTH) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La descripción del rol debe tener mínimo " + MIN_DESCRIPTION_LENGTH + " caracteres"
            );
        }
    }

    private String normalizeText(String value) {
        return value == null ? null : value.trim();
    }
}