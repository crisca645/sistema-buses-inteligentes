package com.iva.ms_security.Repositories;

import com.iva.ms_security.Models.Profile;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProfileRepository extends MongoRepository<Profile, String> {
}
