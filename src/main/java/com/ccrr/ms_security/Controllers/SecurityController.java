package com.ccrr.ms_security.Controllers;

import com.ccrr.ms_security.Models.LoginRequest;
import com.ccrr.ms_security.Models.User;
import com.ccrr.ms_security.Services.PasswordResetService;
import com.ccrr.ms_security.Services.SecurityService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@CrossOrigin
@RestController
@RequestMapping("/security")
public class SecurityController {

    @Autowired
    private SecurityService theSecurityService;

    @Autowired
    private PasswordResetService thePasswordResetService;

    @PostMapping("login")
    public HashMap<String, Object> login(@RequestBody LoginRequest loginRequest,
                                         final HttpServletResponse response) throws IOException {
        HashMap<String, Object> theResponse = new HashMap<>();
        String token = this.theSecurityService.login(loginRequest);
        if (token != null) {
            theResponse.put("token", token);
            theResponse.put("message", "Inicio de sesión exitoso");
        } else {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            theResponse.put("message", "Email o contraseña incorrectos");
        }
        return theResponse;
    }

    @PostMapping("forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String message = this.thePasswordResetService.forgotPassword(email);
        return ResponseEntity.ok(Map.of("message", message));
    }

    @PostMapping("reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> body) {
        try {
            String token = body.get("token");
            String newPassword = body.get("newPassword");
            String confirmPassword = body.get("confirmPassword");
            String message = this.thePasswordResetService.resetPassword(token, newPassword, confirmPassword);
            return ResponseEntity.ok(Map.of("message", message));
        } catch (RuntimeException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("google/callback")
    public ResponseEntity<?> googleCallback(@AuthenticationPrincipal OAuth2User oauthUser) {
        try {
            String email = oauthUser.getAttribute("email");
            String name = oauthUser.getAttribute("given_name");
            String lastname = oauthUser.getAttribute("family_name");

            if (email == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "No se pudo obtener el email del proveedor. Asegúrate de tener un email público."));
            }

            User existingUser = theSecurityService.findOrCreateGoogleUser(email, name, lastname);

            if (existingUser == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("message", "No se pudo crear o encontrar el usuario."));
            }

            String token = theSecurityService.generateTokenForUser(existingUser);

            return ResponseEntity.ok(Map.of(
                    "token", token,
                    "message", "Inicio de sesión exitoso"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", e.getMessage()));
        }
    }
}