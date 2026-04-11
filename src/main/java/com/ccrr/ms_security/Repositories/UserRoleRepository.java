package com.ccrr.ms_security.Repositories;

import com.ccrr.ms_security.Models.UserRole;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface UserRoleRepository extends MongoRepository<UserRole, String> {
    @Query("{ 'user.$id' : ObjectId(?0) }")
    List<UserRole> getRolesByUser(String userId);

    @Query("{ 'role.$id' : ObjectId(?0) }")
    List<UserRole> getUsersByRole(String roleId);

    @Query("{ 'user.$id' : ObjectId(?0), 'role.$id' : ObjectId(?1) }")
    List<UserRole> getUserRolesByUserAndRole(String userId, String roleId);
}