package com.iva.ms_security.Repositories;

import com.iva.ms_security.Models.PasswordResetToken;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface PasswordResetTokenRepository extends MongoRepository<PasswordResetToken, String> {

    @Query("{'token': ?0}")
    PasswordResetToken findByToken(String token);
}