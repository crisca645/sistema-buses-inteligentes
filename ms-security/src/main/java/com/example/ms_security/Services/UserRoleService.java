package com.example.ms_security.Services;

import com.example.ms_security.Models.Role;
import com.example.ms_security.Models.RoleAvailabilityResponse;
import com.example.ms_security.Models.User;
import com.example.ms_security.Models.UserAvailableRolesResponse;
import com.example.ms_security.Models.UserRole;
import com.example.ms_security.Models.UserRoleByUserResponse;
import com.example.ms_security.Models.UserWithRolesResponse;
import com.example.ms_security.Repositories.RoleRepository;
import com.example.ms_security.Repositories.UserRepository;
import com.example.ms_security.Repositories.UserRoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class UserRoleService {

    public enum AddUserRoleResult {
        SUCCESS,
        USER_OR_ROLE_NOT_FOUND,
        EMAIL_FAILED
    }

    public enum RemoveUserRoleResult {
        SUCCESS,
        USER_ROLE_NOT_FOUND,
        EMAIL_FAILED
    }

    @Autowired
    private UserRepository theUserRepository;

    @Autowired
    private RoleRepository theRoleRepository;

    @Autowired
    private UserRoleRepository theUserRoleRepository;

    @Autowired
    private EmailNotificationService theEmailNotificationService;

    public AddUserRoleResult addUserRole(String userId, String roleId) {
        User user = this.theUserRepository.findById(userId).orElse(null);
        Role role = this.theRoleRepository.findById(roleId).orElse(null);

        if (user == null || role == null) {
            return AddUserRoleResult.USER_OR_ROLE_NOT_FOUND;
        }

        UserRole theUserRole = new UserRole(user, role);
        UserRole savedUserRole = this.theUserRoleRepository.save(theUserRole);

        List<Role> currentRoles = this.getCurrentRolesByUser(userId);
        boolean emailSent = this.theEmailNotificationService.sendRoleChangeNotification(
                user,
                currentRoles,
                "Se te asignó el rol \"" + role.getName() + "\""
        );

        if (!emailSent) {
            this.theUserRoleRepository.delete(savedUserRole);
            return AddUserRoleResult.EMAIL_FAILED;
        }

        return AddUserRoleResult.SUCCESS;
    }

    public RemoveUserRoleResult removeUserRole(String userRoleId) {
        UserRole userRole = this.theUserRoleRepository.findById(userRoleId).orElse(null);

        if (userRole == null) {
            return RemoveUserRoleResult.USER_ROLE_NOT_FOUND;
        }

        User user = userRole.getUser();
        Role role = userRole.getRole();

        this.theUserRoleRepository.delete(userRole);

        List<Role> currentRoles = this.getCurrentRolesByUser(user.getId());
        boolean emailSent = this.theEmailNotificationService.sendRoleChangeNotification(
                user,
                currentRoles,
                "Se te removió el rol \"" + role.getName() + "\""
        );

        if (!emailSent) {
            this.theUserRoleRepository.save(userRole);
            return RemoveUserRoleResult.EMAIL_FAILED;
        }

        return RemoveUserRoleResult.SUCCESS;
    }

    public List<UserWithRolesResponse> findUsersWithRoles(String search) {
        List<User> users;

        if (StringUtils.hasText(search)) {
            String normalizedSearch = search.trim();
            users = this.theUserRepository
                    .findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                            normalizedSearch,
                            normalizedSearch
                    );
        } else {
            users = this.theUserRepository.findAll();
        }

        List<UserWithRolesResponse> response = new ArrayList<>();

        for (User user : users) {
            List<Role> roles = this.getCurrentRolesByUser(user.getId());

            response.add(
                    new UserWithRolesResponse(
                            user.getId(),
                            user.getName(),
                            user.getEmail(),
                            roles
                    )
            );
        }

        return response;
    }

    public UserAvailableRolesResponse findAvailableRolesByUser(String userId) {
        User user = this.theUserRepository.findById(userId).orElse(null);

        if (user == null) {
            return null;
        }

        List<Role> allRoles = this.theRoleRepository.findAll();
        List<UserRole> userRoles = this.theUserRoleRepository.getRolesByUser(userId);

        Set<String> assignedRoleIds = new HashSet<>();
        for (UserRole userRole : userRoles) {
            if (userRole.getRole() != null && userRole.getRole().getId() != null) {
                assignedRoleIds.add(userRole.getRole().getId());
            }
        }

        List<RoleAvailabilityResponse> availableRoles = new ArrayList<>();
        for (Role role : allRoles) {
            boolean assigned = assignedRoleIds.contains(role.getId());

            availableRoles.add(
                    new RoleAvailabilityResponse(
                            role.getId(),
                            role.getName(),
                            role.getDescription(),
                            assigned
                    )
            );
        }

        return new UserAvailableRolesResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                availableRoles
        );
    }

    // CAMBIO:
    // Devuelve las relaciones usuario-rol de un usuario, incluyendo el userRoleId,
    // para poder verlo desde Postman sin consultar directamente la base de datos.
    public List<UserRoleByUserResponse> findUserRolesByUser(String userId) {
        User user = this.theUserRepository.findById(userId).orElse(null);

        if (user == null) {
            return null;
        }

        List<UserRole> userRoles = this.theUserRoleRepository.getRolesByUser(userId);
        List<UserRoleByUserResponse> response = new ArrayList<>();

        for (UserRole userRole : userRoles) {
            if (userRole.getRole() != null) {
                Role role = userRole.getRole();

                response.add(
                        new UserRoleByUserResponse(
                                userRole.getId(),
                                role.getId(),
                                role.getName(),
                                role.getDescription()
                        )
                );
            }
        }

        return response;
    }

    private List<Role> getCurrentRolesByUser(String userId) {
        List<UserRole> userRoles = this.theUserRoleRepository.getRolesByUser(userId);
        List<Role> roles = new ArrayList<>();

        for (UserRole userRole : userRoles) {
            if (userRole.getRole() != null) {
                roles.add(userRole.getRole());
            }
        }

        return roles;
    }
}