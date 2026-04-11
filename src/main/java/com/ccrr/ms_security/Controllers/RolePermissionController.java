package com.ccrr.ms_security.Controllers;

import com.ccrr.ms_security.Models.RolePermission;
import com.ccrr.ms_security.Services.RolePermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin
@RestController
@RequestMapping("/role-permission")
public class RolePermissionController {

    @Autowired
    private RolePermissionService theRolePermissionService;

    @GetMapping("role/{roleId}")
    public ResponseEntity<?> getPermissionsByRole(@PathVariable String roleId) {
        List<RolePermission> rolePermissions = this.theRolePermissionService.getPermissionsByRole(roleId);
        if (rolePermissions == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Role not found"));
        }
        return ResponseEntity.ok(rolePermissions);
    }

    @PostMapping({"/{roleId}/permission/{permissionId}", "/role/{roleId}/permission/{permissionId}"})
    public ResponseEntity<Map<String, String>> addRolePermission(
            @PathVariable String roleId,
            @PathVariable String permissionId) {

        RolePermissionService.AddRolePermissionResult response =
                this.theRolePermissionService.addRolePermission(roleId, permissionId);

        if (response == RolePermissionService.AddRolePermissionResult.SUCCESS) {
            return ResponseEntity.ok(Map.of("message", "Success"));
        }

        if (response == RolePermissionService.AddRolePermissionResult.DUPLICATE) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Role already has that permission"));
        }

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "Role or Permission not found"));
    }

    @DeleteMapping("/{rolePermissionId}")
    public ResponseEntity<Map<String, String>> removeRolePermission(@PathVariable String rolePermissionId) {
        boolean response = this.theRolePermissionService.removeRolePermission(rolePermissionId);
        if (response) {
            return ResponseEntity.ok(Map.of("message", "Success"));
        }
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "RolePermission not found"));
    }
}