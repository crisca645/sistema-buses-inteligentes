package com.ccrr.ms_security.Services;

import com.ccrr.ms_security.Models.Role;
import com.ccrr.ms_security.Models.RoleAvailabilityResponse;
import com.ccrr.ms_security.Models.User;
import com.ccrr.ms_security.Models.UserAvailableRolesResponse;
import com.ccrr.ms_security.Models.UserRole;
import com.ccrr.ms_security.Models.UserRoleByUserResponse;
import com.ccrr.ms_security.Models.UserWithRolesResponse;
import com.ccrr.ms_security.Repositories.RoleRepository;
import com.ccrr.ms_security.Repositories.UserRepository;
import com.ccrr.ms_security.Repositories.UserRoleRepository;
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
        DUPLICATE_RELATION,
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
        if (!StringUtils.hasText(userId) || !StringUtils.hasText(roleId)) {
            return AddUserRoleResult.USER_OR_ROLE_NOT_FOUND;
        }

        userId = userId.trim();
        roleId = roleId.trim();

        User user = this.theUserRepository.findById(userId).orElse(null);
        Role role = this.theRoleRepository.findById(roleId).orElse(null);

        if (user == null || role == null) {
            return AddUserRoleResult.USER_OR_ROLE_NOT_FOUND;
        }

        List<UserRole> existingRelations = this.theUserRoleRepository.getUserRolesByUserAndRole(userId, roleId);
        if (existingRelations != null && !existingRelations.isEmpty()) {
            return AddUserRoleResult.DUPLICATE_RELATION;
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
        if (!StringUtils.hasText(userRoleId)) {
            return RemoveUserRoleResult.USER_ROLE_NOT_FOUND;
        }

        UserRole userRole = this.theUserRoleRepository.findById(userRoleId.trim()).orElse(null);

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
            users = this.theUserRepository.findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                    normalizedSearch,
                    normalizedSearch
            );
        } else {
            users = this.theUserRepository.findAll();
        }

        List<UserWithRolesResponse> response = new ArrayList<>();
        for (User user : users) {
            response.add(new UserWithRolesResponse(
                    user.getId(),
                    user.getName(),
                    user.getEmail(),
                    this.getCurrentRolesByUser(user.getId())
            ));
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
            availableRoles.add(new RoleAvailabilityResponse(
                    role.getId(),
                    role.getName(),
                    role.getDescription(),
                    assignedRoleIds.contains(role.getId())
            ));
        }

        return new UserAvailableRolesResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                availableRoles
        );
    }

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
                response.add(new UserRoleByUserResponse(
                        userRole.getId(),
                        role.getId(),
                        role.getName(),
                        role.getDescription()
                ));
            }
        }

        return response;
    }

    private List<Role> getCurrentRolesByUser(String userId) {
        List<UserRole> userRoles = this.theUserRoleRepository.getRolesByUser(userId);
        List<Role> currentRoles = new ArrayList<>();

        for (UserRole userRole : userRoles) {
            if (userRole.getRole() != null) {
                currentRoles.add(userRole.getRole());
            }
        }

        return currentRoles;
    }
}