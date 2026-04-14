package com.ccrr.ms_security.Repositories;

import com.ccrr.ms_security.Models.PasswordResetToken;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface PasswordResetTokenRepository extends MongoRepository<PasswordResetToken, String> {

    @Query("{'token': ?0}")
    PasswordResetToken findByToken(String token);
}
