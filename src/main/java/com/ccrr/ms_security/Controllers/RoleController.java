package com.ccrr.ms_security.Controllers;

import com.ccrr.ms_security.Models.Role;
import com.ccrr.ms_security.Services.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin
@RestController
@RequestMapping("/roles")
public class RoleController {

    @Autowired
    private RoleService theRoleService;

    @GetMapping("")
    public List<Role> find() {
        return this.theRoleService.find();
    }

    @GetMapping("{id}")
    public Role findById(@PathVariable String id) {
        return this.theRoleService.findById(id);
    }

    @PostMapping
    public ResponseEntity<Role> create(@RequestBody Role newRole) {
        Role createdRole = this.theRoleService.create(newRole);
        return new ResponseEntity<>(createdRole, HttpStatus.CREATED);
    }

    @PutMapping("{id}")
    public Role update(@PathVariable String id, @RequestBody Role newRole) {
        return this.theRoleService.update(id, newRole);
    }

    @DeleteMapping("{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable String id) {
        RoleService.DeleteRoleResult response = this.theRoleService.delete(id);

        if (response == RoleService.DeleteRoleResult.SUCCESS) {
            return ResponseEntity.ok(Map.of("message", "Success"));
        }

        if (response == RoleService.DeleteRoleResult.ROLE_ASSIGNED_TO_USERS) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Cannot delete role because it has users assigned"));
        }

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "Role not found"));
    }
}