package com.example.ms_security.Services;

import com.example.ms_security.Models.Permission;
import com.example.ms_security.Models.Role;
import com.example.ms_security.Models.RolePermission;
import com.example.ms_security.Models.User;
import com.example.ms_security.Models.UserRole;
import com.example.ms_security.Repositories.PermissionRepository;
import com.example.ms_security.Repositories.RolePermissionRepository;
import com.example.ms_security.Repositories.UserRepository;
import com.example.ms_security.Repositories.UserRoleRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ValidatorsService {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PermissionRepository thePermissionRepository;

    @Autowired
    private UserRepository theUserRepository;

    @Autowired
    private RolePermissionRepository theRolePermissionRepository;

    @Autowired
    private UserRoleRepository theUserRoleRepository;

    private static final String BEARER_PREFIX = "Bearer ";

    public boolean validationRolePermission(HttpServletRequest request, String url, String method) {
        boolean success = false;
        User theUser = this.getUser(request);

        if (theUser != null) {
            // CAMBIO:
            // Se normaliza la URL reemplazando ids Mongo y números por "?"
            // para que coincida con permisos parametrizados.
            url = url.replaceAll("[0-9a-fA-F]{24}|\\d+", "?");

            Permission thePermission = this.thePermissionRepository.getPermission(url, method);
            List<UserRole> roles = this.theUserRoleRepository.getRolesByUser(theUser.getId());

            int i = 0;
            while (i < roles.size() && !success) {
                UserRole actual = roles.get(i);
                Role theRole = actual.getRole();

                if (theRole != null && thePermission != null) {
                    // CAMBIO:
                    // Ya no dependemos de una consulta singular.
                    // Basta con que exista al menos una coincidencia.
                    List<RolePermission> rolePermissions =
                            this.theRolePermissionRepository.getRolePermissions(
                                    theRole.getId(),
                                    thePermission.getId()
                            );

                    if (rolePermissions != null && !rolePermissions.isEmpty()) {
                        success = true;
                    }
                } else {
                    success = false;
                }
                i += 1;
            }
        }

        return success;
    }

    public User getUser(final HttpServletRequest request) {
        User theUser = null;
        String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader != null && authorizationHeader.startsWith(BEARER_PREFIX)) {
            String token = authorizationHeader.substring(BEARER_PREFIX.length());
            User theUserFromToken = jwtService.getUserFromToken(token);

            if (theUserFromToken != null) {
                theUser = this.theUserRepository.findById(theUserFromToken.getId()).orElse(null);
            }
        }

        return theUser;
    }
}