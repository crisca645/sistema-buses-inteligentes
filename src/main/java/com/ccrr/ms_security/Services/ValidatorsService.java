package com.ccrr.ms_security.Services;

import com.ccrr.ms_security.Models.Permission;
import com.ccrr.ms_security.Models.Role;
import com.ccrr.ms_security.Models.RolePermission;
import com.ccrr.ms_security.Models.Session;
import com.ccrr.ms_security.Models.User;
import com.ccrr.ms_security.Models.UserRole;
import com.ccrr.ms_security.Repositories.PermissionRepository;
import com.ccrr.ms_security.Repositories.RolePermissionRepository;
import com.ccrr.ms_security.Repositories.SessionRepository;
import com.ccrr.ms_security.Repositories.UserRepository;
import com.ccrr.ms_security.Repositories.UserRoleRepository;
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

    public boolean validationRolePermission(HttpServletRequest request, String url, String method) {
        boolean success = false;
        User theUser = this.getUser(request);
        if (theUser != null) {
            url = url.replaceAll("[0-9a-fA-F]{24}|\\d+", "?");
            Permission thePermission = this.thePermissionRepository.getPermission(url, method);
            List<UserRole> roles = this.theUserRoleRepository.getRolesByUser(theUser.getId());
            int i = 0;
            while (i < roles.size() && !success) {
                UserRole actual = roles.get(i);
                Role theRole = actual.getRole();
                if (theRole != null && thePermission != null) {
                    List<RolePermission> rolePermissions = this.theRolePermissionRepository
                            .getRolePermissions(theRole.getId(), thePermission.getId());
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
        String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            return null;
        }

        String token = authorizationHeader.substring(BEARER_PREFIX.length());
        if (!jwtService.validateToken(token)) {
            return null;
        }

        Session theSession = this.theSessionRepository.findByToken(token);
        if (theSession == null || theSession.getExpiration() == null || theSession.getExpiration().before(new Date())) {
            return null;
        }

        if (theSession.getCode2FA() != null) {
            return null;
        }

        User tokenUser = jwtService.getUserFromToken(token);
        if (tokenUser == null) {
            return null;
        }

        return this.theUserRepository.findById(tokenUser.getId()).orElse(null);
    }
}