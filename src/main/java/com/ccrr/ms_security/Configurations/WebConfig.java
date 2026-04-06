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
<<<<<<< HEAD
                .addPathPatterns("/api/**") //controla todo lo que empiesa por /api se valida por el interceptor
                .excludePathPatterns("/api/public/**");
=======
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/security/login",
                        "/users/register",
                        "/error",
                        "/permissions",
                        "/permissions/**",
                        "/roles",
                        "/roles/**",
                        "/role-permission/**",
                        "/user-role/**",
                        "/security/forgot-password",
                        "/security/reset-password",
                        "/security/google/callback"
>>>>>>> origin/feature/login-registro-sesion

                        );

    }
}
