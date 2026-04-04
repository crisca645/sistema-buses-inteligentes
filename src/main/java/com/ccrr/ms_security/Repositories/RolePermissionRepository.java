package com.ccrr.ms_security.Repositories;

import com.ccrr.ms_security.Models.RolePermission;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface RolePermissionRepository extends MongoRepository<RolePermission, String> {
    @Query("{'role.$id': ObjectId(?0)}")
    List<RolePermission> getPermissionsByRole(String roleId);
    @Query("{'role.$id': ObjectId(?0),'permission.$id': ObjectId(?1)}")
    public RolePermission getRolePermission(String roleId,String permissionId);
}


//public interface RolePermissionRepository extends MongoRepository<RolePermission, String>
  //// el MongoRepository es el que tiene impemnetadas los metodos get,post,put y delete por defecto
  ///  por eso se hace extend de eso para poder usarlos

    /// <RolePermission, String> estos argumentos  el RolePermission (esta en modells) significa la entidad  la cual
    /// va a manejar y en la cual va a usar los metodos get put post delete update etc

    ///  y el segundo argumento que es el string es el tipo de dato que tiene el identificador
    ///  en RolePermission ( en modells) si vamos a RolePermision en modells el id es string
