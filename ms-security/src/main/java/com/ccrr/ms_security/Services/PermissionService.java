package com.ccrr.ms_security.Services;

import com.ccrr.ms_security.Models.Permission;
import com.ccrr.ms_security.Repositories.PermissionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PermissionService {

    @Autowired
    private PermissionRepository thePermissionRepository;

    public List<Permission> find() {
        return this.thePermissionRepository.findAll();
    }

    public Permission findById(String id) {
        return this.thePermissionRepository.findById(id).orElse(null);
    }

    public Permission create(Permission newPermission) {
        return this.thePermissionRepository.save(newPermission);
    }

    public Permission update(String id, Permission newPermission) {
        Permission actualPermission = this.findById(id);
        if (actualPermission != null) {
            actualPermission.setUrl(newPermission.getUrl());
            actualPermission.setMethod(newPermission.getMethod());
            actualPermission.setModel(newPermission.getModel());
            return this.thePermissionRepository.save(actualPermission);
        }
        return null;
    }

    public void delete(String id) {
        this.thePermissionRepository.deleteById(id);
    }
}