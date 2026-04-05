package com.iva.ms_security.Repositories;

import com.iva.ms_security.Models.Permission;
import com.iva.ms_security.Models.RolePermission;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface  PermissionRepository extends   MongoRepository<Permission,String> {
    @Query("{'url':?0,'method':?1}")
    Permission getPermission(String url,
                             String method);
}