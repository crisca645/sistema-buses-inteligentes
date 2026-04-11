package com.ccrr.ms_security.Services;

import com.ccrr.ms_security.Models.Session;
import com.ccrr.ms_security.Repositories.SessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SessionService {
    @Autowired
    private SessionRepository theSessionRepository;

    public List<Session> find() {
        return this.theSessionRepository.findAll();
    }

    public Session findById(String id) {
        return this.theSessionRepository.findById(id).orElse(null);
    }

    public Session create(Session newSession) {
        return this.theSessionRepository.save(newSession);
    }

    public Session update(String id, Session newSession) {
        Session actualSession = this.theSessionRepository.findById(id).orElse(null);
        if (actualSession == null) {
            return null;
        }

        actualSession.setToken(newSession.getToken());
        actualSession.setExpiration(newSession.getExpiration());
        actualSession.setCode2FA(newSession.getCode2FA());
        actualSession.setFailedAttempts(newSession.getFailedAttempts());
        actualSession.setUser(newSession.getUser());
        this.theSessionRepository.save(actualSession);
        return actualSession;
    }

    public void delete(String id) {
        Session theSession = this.theSessionRepository.findById(id).orElse(null);
        if (theSession != null) {
            this.theSessionRepository.delete(theSession);
        }
    }
}