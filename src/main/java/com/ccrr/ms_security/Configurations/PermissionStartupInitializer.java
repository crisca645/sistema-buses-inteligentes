package com.ccrr.ms_security.Configurations;

import com.ccrr.ms_security.Services.PermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class PermissionStartupInitializer implements CommandLineRunner {

    @Autowired
    private PermissionService thePermissionService;

    @Override
    public void run(String... args) {
        // CAMBIO: al iniciar la aplicación, asegura que exista
        // el catálogo base de permisos del criterio 4.
        this.thePermissionService.ensureDefaultPermissions();
    }
}
