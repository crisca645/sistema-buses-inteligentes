package com.ccrr.ms_security.Controllers;

import com.ccrr.ms_security.Models.UserAvailableRolesResponse;
import com.ccrr.ms_security.Models.UserRoleByUserResponse;
import com.ccrr.ms_security.Models.UserWithRolesResponse;
import com.ccrr.ms_security.Services.UserRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin
@RestController
@RequestMapping("/user-role")
public class UserRoleController {
    @Autowired
    private UserRoleService theUserRoleService;

    @GetMapping("users-with-roles")
    public List<UserWithRolesResponse> findUsersWithRoles(@RequestParam(required = false) String search) {
        return this.theUserRoleService.findUsersWithRoles(search);
    }

    @GetMapping("user/{userId}/available-roles")
    public ResponseEntity<?> findAvailableRolesByUser(@PathVariable String userId) {
        UserAvailableRolesResponse response = this.theUserRoleService.findAvailableRolesByUser(userId);
        if (response == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "User not found"));
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("user/{userId}/roles")
    public ResponseEntity<?> findUserRolesByUser(@PathVariable String userId) {
        List<UserRoleByUserResponse> response = this.theUserRoleService.findUserRolesByUser(userId);
        if (response == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "User not found"));
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping({"/{userId}/role/{roleId}", "/user/{userId}/role/{roleId}"})
    public ResponseEntity<Map<String, String>> addUserRole(
            @PathVariable String userId,
            @PathVariable String roleId) {

        UserRoleService.AddUserRoleResult response = this.theUserRoleService.addUserRole(userId, roleId);

        if (response == UserRoleService.AddUserRoleResult.SUCCESS) {
            return ResponseEntity.ok(Map.of("message", "Success"));
        }

        if (response == UserRoleService.AddUserRoleResult.DUPLICATE_RELATION) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "User already has that role"));
        }

        if (response == UserRoleService.AddUserRoleResult.EMAIL_FAILED) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Could not send notification email"));
        }

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "User or Role not found"));
    }

    @DeleteMapping("{userRoleId}")
    public ResponseEntity<Map<String, String>> removeUserRole(@PathVariable String userRoleId) {
        UserRoleService.RemoveUserRoleResult response = this.theUserRoleService.removeUserRole(userRoleId);
        if (response == UserRoleService.RemoveUserRoleResult.SUCCESS) {
            return ResponseEntity.ok(Map.of("message", "Success"));
        }
        if (response == UserRoleService.RemoveUserRoleResult.EMAIL_FAILED) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Could not send notification email"));
        }
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "User or Role not found"));
    }
}