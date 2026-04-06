package com.ccrr.ms_security.Repositories;

import com.ccrr.ms_security.Models.Permission;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface PermissionRepository extends MongoRepository<Permission, String> {

    @Query("{'url':?0,'method':?1}")
    Permission getPermission(String url, String method);

    Permission findByMethodAndUrl(String method, String url);
}