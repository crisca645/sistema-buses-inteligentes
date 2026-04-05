package com.example.ms_security.Services;

import com.example.ms_security.Models.Permission;
import com.example.ms_security.Repositories.PermissionRepository;
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

    // CAMBIO: asegura el catálogo base de permisos requerido por el criterio 4.
    public void ensureDefaultPermissions() {
        // Gestión de usuarios
        this.createPermissionIfMissing("/api/usuarios", "GET", "usuarios");
        this.createPermissionIfMissing("/api/usuarios", "POST", "usuarios");
        this.createPermissionIfMissing("/api/usuarios", "PUT", "usuarios");
        this.createPermissionIfMissing("/api/usuarios", "DELETE", "usuarios");

        // Gestión de buses
        this.createPermissionIfMissing("/api/buses", "GET", "buses");
        this.createPermissionIfMissing("/api/buses", "POST", "buses");
        this.createPermissionIfMissing("/api/buses", "PUT", "buses");
        this.createPermissionIfMissing("/api/buses", "DELETE", "buses");

        // Gestión de rutas
        this.createPermissionIfMissing("/api/rutas", "GET", "rutas");
        this.createPermissionIfMissing("/api/rutas", "POST", "rutas");
        this.createPermissionIfMissing("/api/rutas", "PUT", "rutas");
        this.createPermissionIfMissing("/api/rutas", "DELETE", "rutas");

        // Gestión de programaciones
        this.createPermissionIfMissing("/api/programaciones", "GET", "programaciones");
        this.createPermissionIfMissing("/api/programaciones", "POST", "programaciones");
        this.createPermissionIfMissing("/api/programaciones", "PUT", "programaciones");
        this.createPermissionIfMissing("/api/programaciones", "DELETE", "programaciones");

        // Visualización de reportes
        this.createPermissionIfMissing("/api/reportes", "GET", "reportes");

        // Gestión de incidentes
        this.createPermissionIfMissing("/api/incidentes", "GET", "incidentes");
        this.createPermissionIfMissing("/api/incidentes", "POST", "incidentes");
        this.createPermissionIfMissing("/api/incidentes", "PUT", "incidentes");
        this.createPermissionIfMissing("/api/incidentes", "DELETE", "incidentes");

        // Envío de mensajes masivos
        this.createPermissionIfMissing("/api/mensajes-masivos", "POST", "mensajes-masivos");
    }

    // CAMBIO: crea el permiso solo si aún no existe por url y método.
    private void createPermissionIfMissing(String url, String method, String model) {
        if (!this.thePermissionRepository.existsByUrlAndMethod(url, method)) {
            Permission permission = new Permission(url, method, model);
            this.thePermissionRepository.save(permission);
        }
    }
}