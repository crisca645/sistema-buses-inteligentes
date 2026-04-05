package com.iva.ms_security.Repositories;

import com.iva.ms_security.Models.Role;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface RoleRepository extends MongoRepository<Role, String> {
}
