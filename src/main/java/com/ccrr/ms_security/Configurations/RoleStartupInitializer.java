package com.ccrr.ms_security.Configurations;

import com.ccrr.ms_security.Services.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class RoleStartupInitializer implements CommandLineRunner {

    @Autowired
    private RoleService theRoleService;

    @Override
    public void run(String... args) {
        // CAMBIO NUEVO:
        // Al arrancar la aplicación, se asegura la existencia
        // de los 5 roles predeterminados del criterio 2.
        this.theRoleService.ensureDefaultRoles();
    }
}