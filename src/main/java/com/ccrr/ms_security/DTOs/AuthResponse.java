package com.ccrr.ms_security.DTOs;

import com.ccrr.ms_security.Models.User;

public class AuthResponse {

    private String token;
    private User user;
    private boolean isNewUser;
    private boolean requiresAdditionalInfo;
    private boolean emailRequired;

    public AuthResponse() {
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public boolean isNewUser() {
        return isNewUser;
    }

    public void setNewUser(boolean newUser) {
        isNewUser = newUser;
    }

    public boolean isRequiresAdditionalInfo() {
        return requiresAdditionalInfo;
    }

    public void setRequiresAdditionalInfo(boolean requiresAdditionalInfo) {
        this.requiresAdditionalInfo = requiresAdditionalInfo;
    }

    public boolean isEmailRequired() {
        return emailRequired;
    }

    public void setEmailRequired(boolean emailRequired) {
        this.emailRequired = emailRequired;
    }
}