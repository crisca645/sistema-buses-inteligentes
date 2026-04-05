package com.iva.ms_security.Interceptors;

import com.iva.ms_security.Models.User;
import com.iva.ms_security.Services.ValidatorsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

@Component
public class SecurityInterceptor implements HandlerInterceptor {
    @Autowired
    private ValidatorsService validatorService;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        // 1) Verifica autenticación/sesión básica
        User user = this.validatorService.getUser(request);
        if (user == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"message\":\"Sesión expirada o inválida\"}");
            return false;
        }

        // 2) Verifica autorización por URL + método
        boolean hasPermission = this.validatorService.validationRolePermission(
                request,
                request.getRequestURI(),
                request.getMethod()
        );

        if (!hasPermission) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN); // 403
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"message\":\"Acceso denegado\"}");
            return false;
        }

        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
                           ModelAndView modelAndView) throws Exception {
        // Lógica a ejecutar después de que se haya manejado la solicitud por el controlador
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
                                Exception ex) throws Exception {
        // Lógica a ejecutar después de completar la solicitud, incluso después de la renderización de la vista
    }
}
