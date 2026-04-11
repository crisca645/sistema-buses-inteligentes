package com.ccrr.ms_security.Repositories;

import com.ccrr.ms_security.Models.Session;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface SessionRepository extends MongoRepository <Session, String> {

    @Query("{'token': ?0}")
    Session findByToken(String token);
}
