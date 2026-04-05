package com.iva.ms_security.Services;

import com.iva.ms_security.Models.LoginRequest;
import com.iva.ms_security.Models.Session;
import com.iva.ms_security.Models.User;
import com.iva.ms_security.Repositories.SessionRepository;
import com.iva.ms_security.Repositories.UserRepository;
import com.iva.ms_security.Repositories.UserRoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.UUID;
import java.util.List;
import java.util.stream.Collectors;
import com.iva.ms_security.Models.UserRole;

import java.util.Date;

@Service
public class SecurityService {

    @Autowired
    private UserRoleRepository theUserRoleRepository;

    @Autowired
    private UserRepository theUserRepository;

    @Autowired
    private SessionRepository theSessionRepository;

    @Autowired
    private EncryptionService theEncryptionService;


    @Autowired
    private JwtService theJwtService;

    public String login(LoginRequest loginRequest) {
        if (loginRequest == null) {
            return null;
        }

        String email = loginRequest.getEmail() != null ? loginRequest.getEmail().trim().toLowerCase() : null;
        String password = loginRequest.getPassword();

        if (email == null || email.isEmpty()) {
            return null;
        }

        if (password == null || password.isEmpty()) {
            return null;
        }

        User theActualUser = this.theUserRepository.getUserByEmail(email);

        if (theActualUser != null &&
                theActualUser.getPassword().equals(theEncryptionService.convertSHA256(password))) {

            String roleName = "USER"; // Aquí puedes obtener el nombre del rol del usuario desde la base de datos si es necesario
            String token = theJwtService.generateToken(theActualUser, roleName);

            Session newSession = new Session();
            newSession.setToken(token);
            newSession.setUser(theActualUser);
            newSession.setCode2FA(null);

            Date expirationDate = new Date(System.currentTimeMillis() + 3600000);
            newSession.setExpiration(expirationDate);

            this.theSessionRepository.save(newSession);

            return token;
        }

        return null;
    }
    public User findOrCreateGoogleUser(String email, String name, String lastname) {
        // Buscar si ya existe
        User existingUser = theUserRepository.getUserByEmail(email.trim().toLowerCase());

        if (existingUser != null) {
            return existingUser;
        }

        // Si no existe, crear cuenta automáticamente
        User newUser = new User();
        newUser.setEmail(email.trim().toLowerCase());
        newUser.setName(name != null ? name : "Usuario");
        newUser.setLastname(lastname != null ? lastname : "Google");
        newUser.setPassword(theEncryptionService.convertSHA256(UUID.randomUUID().toString()));

        return theUserRepository.save(newUser);
    }

    public String generateTokenForUser(User user) {
        List<UserRole> userRoles = theUserRoleRepository.getRolesByUser(user.getId());
        String roleName = userRoles.stream()
                .filter(ur -> ur.getRole() != null)
                .map(ur -> ur.getRole().getName())
                .collect(Collectors.joining(","));
        if (roleName.isEmpty()) roleName = "USER";

        return theJwtService.generateToken(user, roleName);
    }
}
    /*
    public boolean permissionsValidation(final HttpServletRequest request,
                                         @RequestBody Permission thePermission) {
        boolean success=this.theValidatorsService.validationRolePermission(request,thePermission.getUrl(),thePermission.getMethod());
        return success;
    }
    */


