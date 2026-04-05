package com.example.ms_security.Repositories;

import com.example.ms_security.Models.Role;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface RoleRepository extends MongoRepository<Role,String> {
    // CAMBIO: permite validar si ya existe un nombre de rol sin importar mayúsculas/minúsculas.
//    boolean existsByNameIgnoreCase(String name);

    // CAMBIO: permite buscar un rol por nombre para validar correctamente en update
    // y no bloquear el mismo registro cuando mantiene su nombre actual.
//    Optional<Role> findByNameIgnoreCase(String name);
}
