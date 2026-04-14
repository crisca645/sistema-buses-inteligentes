package com.ccrr.ms_security.DTOs;

import com.ccrr.ms_security.Models.User;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {

    private String token;
    private User user;
    private boolean isNewUser;
    private boolean requiresAdditionalInfo;
    private boolean emailRequired;

    /** GitHub sin email: no hay usuario en BD aún; el frontend debe pedir email alternativo. */
    private Boolean requiresEmailCompletion;

    @JsonProperty("message")
    private String completionMessage;

    @JsonProperty("authProvider")
    private String pendingAuthProvider;

    @JsonProperty("providerId")
    private String pendingProviderId;

    @JsonProperty("username")
    private String pendingUsername;

    @JsonProperty("name")
    private String pendingName;

    @JsonProperty("picture")
    private String pendingPicture;

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

    public Boolean getRequiresEmailCompletion() {
        return requiresEmailCompletion;
    }

    public void setRequiresEmailCompletion(Boolean requiresEmailCompletion) {
        this.requiresEmailCompletion = requiresEmailCompletion;
    }

    public boolean isRequiresEmailCompletion() {
        return Boolean.TRUE.equals(requiresEmailCompletion);
    }

    public String getCompletionMessage() {
        return completionMessage;
    }

    public void setCompletionMessage(String completionMessage) {
        this.completionMessage = completionMessage;
    }

    public String getPendingAuthProvider() {
        return pendingAuthProvider;
    }

    public void setPendingAuthProvider(String pendingAuthProvider) {
        this.pendingAuthProvider = pendingAuthProvider;
    }

    public String getPendingProviderId() {
        return pendingProviderId;
    }

    public void setPendingProviderId(String pendingProviderId) {
        this.pendingProviderId = pendingProviderId;
    }

    public String getPendingUsername() {
        return pendingUsername;
    }

    public void setPendingUsername(String pendingUsername) {
        this.pendingUsername = pendingUsername;
    }

    public String getPendingName() {
        return pendingName;
    }

    public void setPendingName(String pendingName) {
        this.pendingName = pendingName;
    }

    public String getPendingPicture() {
        return pendingPicture;
    }

    public void setPendingPicture(String pendingPicture) {
        this.pendingPicture = pendingPicture;
    }
}
