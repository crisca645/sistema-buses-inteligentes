package com.ccrr.ms_security.Repositories;

import com.ccrr.ms_security.Models.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface UserRepository extends MongoRepository <User,String> {
    @Query("{'email': ?0}")
    public User getUserByEmail(String email);


    @Query("{'providerId': ?0}")
    public User getUserByProviderId(String providerId);  //Sirve para buscar directamente por el id del proveedor.

    @Query("{'authProvider': ?0, 'providerId': ?1}")
    public User getUserByAuthProviderAndProviderId(String authProvider, String providerId);

    //Este es el más importante para OAuth.
    //
    //Ejemplo:
    //
    //authProvider = "GOOGLE"
    //
    //providerId = "1092837465..."
    //
    //Con eso sabes si ese usuario ya había iniciado con Google antes.

}