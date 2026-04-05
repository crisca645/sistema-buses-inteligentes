package com.example.ms_security.Services;

import com.example.ms_security.Models.Profile;
import com.example.ms_security.Models.Session;
import com.example.ms_security.Models.User;
import com.example.ms_security.Repositories.ProfileRepository;
import com.example.ms_security.Repositories.SessionRepository;
import com.example.ms_security.Repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    @Autowired
    private UserRepository theUserRepository;

    @Autowired
    private SessionRepository theSessionRepository;

    @Autowired
    private ProfileRepository theProfileRepository;

    @Autowired
    private EncryptionService theEncryptionService;

    public List<User> find() {
        return this.theUserRepository.findAll();
    }

    public User findById(String id) {
        return this.theUserRepository.findById(id).orElse(null);
    }

    public User create(User newUser) {
        if (newUser.getPassword() != null && !newUser.getPassword().isEmpty()) {
            newUser.setPassword(this.theEncryptionService.convertSHA256(newUser.getPassword()));
        }
        return this.theUserRepository.save(newUser);
    }

    public User update(String id, User newUser) {
        User actualUser = this.theUserRepository.findById(id).orElse(null);

        if (actualUser != null) {
            actualUser.setName(newUser.getName());
            actualUser.setEmail(newUser.getEmail());

            if (newUser.getPassword() != null && !newUser.getPassword().isEmpty()) {
                actualUser.setPassword(this.theEncryptionService.convertSHA256(newUser.getPassword()));
            }

            this.theUserRepository.save(actualUser);
            return actualUser;
        } else {
            return null;
        }
    }

    public void delete(String id) {
        User theUser = this.theUserRepository.findById(id).orElse(null);
        if (theUser != null) {
            this.theUserRepository.delete(theUser);
        }
    }

    public boolean addSession(String userId, String sessionId) {
        User theUser = this.theUserRepository.findById(userId).orElse(null);
        Session theSession = this.theSessionRepository.findById(sessionId).orElse(null);
        if (theUser != null && theSession != null) {
            theSession.setUser(theUser);
            this.theSessionRepository.save(theSession);
            return true;
        } else {
            return false;
        }
    }

    public boolean removeSession(String userId, String sessionId) {
        User theUser = this.theUserRepository.findById(userId).orElse(null);
        Session theSession = this.theSessionRepository.findById(sessionId).orElse(null);
        if (theUser != null && theSession != null) {
            theSession.setUser(null);
            this.theSessionRepository.save(theSession);
            return true;
        } else {
            return false;
        }
    }

    public boolean addProfile(String userId, String profileId) {
        User theUser = this.theUserRepository.findById(userId).orElse(null);
        Profile theProfile = this.theProfileRepository.findById(profileId).orElse(null);
        if (theUser != null && theProfile != null) {
            theProfile.setUser(theUser);
            this.theProfileRepository.save(theProfile);
            return true;
        } else {
            return false;
        }
    }

    public boolean removeProfile(String userId, String profileId) {
        User theUser = this.theUserRepository.findById(userId).orElse(null);
        Profile theProfile = this.theProfileRepository.findById(profileId).orElse(null);
        if (theUser != null && theProfile != null) {
            theProfile.setUser(null);
            this.theProfileRepository.save(theProfile);
            return true;
        } else {
            return false;
        }
    }
}