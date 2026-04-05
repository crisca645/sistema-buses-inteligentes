package com.example.ms_security.Repositories;

import com.example.ms_security.Models.Permission;
import com.example.ms_security.Models.RolePermission;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface  PermissionRepository extends   MongoRepository<Permission,String> {
    @Query("{'url':?0,'method':?1}")
    Permission getPermission(String url,
                             String method);

    // CAMBIO: permite validar si ya existe un permiso por url y método
    // antes de insertarlo en la carga inicial.
    boolean existsByUrlAndMethod(String url, String method);
}
