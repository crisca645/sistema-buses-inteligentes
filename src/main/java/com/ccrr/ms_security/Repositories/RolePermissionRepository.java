package com.ccrr.ms_security.Repositories;

import com.ccrr.ms_security.Models.RolePermission;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface RolePermissionRepository extends MongoRepository<RolePermission, String> {

    @Query("{'role.$id': ObjectId(?0)}")
    List<RolePermission> getPermissionsByRole(String roleId);

    // CAMBIO:
    // Se reemplaza la consulta singular por una consulta que devuelve lista.
    // Esto evita riesgos si por cualquier motivo existen duplicados históricos
    // de la misma relación rol-permiso en base de datos.
    @Query("{'role.$id': ObjectId(?0),'permission.$id': ObjectId(?1)}")
    List<RolePermission> getRolePermissions(String roleId, String permissionId);
}
