package com.ccrr.ms_security.Configurations;

import com.ccrr.ms_security.Interceptors.SecurityInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private SecurityInterceptor securityInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(securityInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/security/login",
                        "/security/forgot-password",
                        "/security/reset-password",
                        "/security/complete-profile",
                        "/security/set-password",
                        "/security/unlink/google",
                        "/security/google/callback",
                        "/security/2fa/verify",
                        "/security/2fa/resend",
                        "/security/2fa/cancel",
                        "/users/register",
                        "/error",
                        "/permissions",
                        "/permissions/**",
                        "/roles",
                        "/roles/**",
                        "/role-permission/**",
                        "/user-role/**",
                        "/oauth2/**",
                        "/login/**",
                        "/api/public/**",
                        "/security/github/complete-email",
                        "/security/unlink/github"
                );
    }
}