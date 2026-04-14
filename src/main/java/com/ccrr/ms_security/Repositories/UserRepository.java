package com.ccrr.ms_security.Repositories;

import com.ccrr.ms_security.Models.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface UserRepository extends MongoRepository<User, String> {
    @Query("{'email': ?0}")
    User getUserByEmail(String email);

    @Query("{'providerId': ?0}")
    User getUserByProviderId(String providerId);

    @Query("{'authProvider': ?0, 'providerId': ?1}")
    User getUserByAuthProviderAndProviderId(String authProvider, String providerId);

    List<User> findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(String name, String email);

    List<User> findByNameContainingIgnoreCaseOrLastnameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String namePart,
            String lastnamePart,
            String emailPart);

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