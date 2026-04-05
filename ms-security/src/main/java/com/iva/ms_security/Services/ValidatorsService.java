package com.iva.ms_security.Services;

import com.iva.ms_security.Repositories.SessionRepository;

import com.iva.ms_security.Models.*;
import com.iva.ms_security.Repositories.PermissionRepository;
import com.iva.ms_security.Repositories.RolePermissionRepository;
import com.iva.ms_security.Repositories.UserRepository;
import com.iva.ms_security.Repositories.UserRoleRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class ValidatorsService {
    @Autowired
    private JwtService jwtService;
    @Autowired
    private SessionRepository theSessionRepository;

    @Autowired
    private PermissionRepository thePermissionRepository;
    @Autowired
    private UserRepository theUserRepository;
    @Autowired
    private RolePermissionRepository theRolePermissionRepository;

    @Autowired
    private UserRoleRepository theUserRoleRepository;

    private static final String BEARER_PREFIX = "Bearer ";

    public boolean validationRolePermission(HttpServletRequest request,
                                            String url,
                                            String method) {
        boolean success = false;
        User theUser = this.getUser(request);
        if (theUser != null) {
            System.out.println("Antes URL " + url + " metodo " + method);
            url = url.replaceAll("[0-9a-fA-F]{24}|\\d+", "?");
            System.out.println("URL " + url + " metodo " + method);
            Permission thePermission = this.thePermissionRepository.getPermission(url, method);

            List<UserRole> roles = this.theUserRoleRepository.getRolesByUser(theUser.getId());
            int i = 0;
            while (i < roles.size() && success == false) {
                UserRole actual = roles.get(i);
                Role theRole = actual.getRole();
                if (theRole != null && thePermission != null) {
                    System.out.println("Rol " + theRole.getId() + " Permission " + thePermission.getId());
                    RolePermission theRolePermission = this.theRolePermissionRepository.getRolePermission(theRole.getId(), thePermission.getId());
                    if (theRolePermission != null) {
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
        String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            return null;
        }

        String token = authorizationHeader.substring(BEARER_PREFIX.length());

        // 1) JWT válido criptográficamente y no expirado
        if (!jwtService.validateToken(token)) {
            return null;
        }

        // 2) Token debe existir en sesión activa de BD
        Session theSession = this.theSessionRepository.findByToken(token);
        if (theSession == null || theSession.getExpiration() == null || theSession.getExpiration().before(new Date())) {
            return null;
        }

        // 3) Usuario del token debe existir
        User tokenUser = jwtService.getUserFromToken(token);
        if (tokenUser == null) {
            return null;
        }

        return this.theUserRepository.findById(tokenUser.getId()).orElse(null);
    }
}