package com.ccrr.ms_security.Controllers;

import com.ccrr.ms_security.DTOs.AuthResponse;
import com.ccrr.ms_security.DTOs.GoogleLoginRequest;
import com.ccrr.ms_security.Models.User;
import com.ccrr.ms_security.Services.GoogleAuthService;
import com.ccrr.ms_security.Services.SecurityService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;

@CrossOrigin
@RestController
@RequestMapping("/security")
public class SecurityController {

    @Autowired
    private SecurityService theSecurityService;

    @Autowired
    private GoogleAuthService googleAuthService;

    @PostMapping("login")
    public HashMap<String, Object> login(@RequestBody User theNewUser,
                                         final HttpServletResponse response) throws IOException {
        HashMap<String, Object> theResponse = new HashMap<>();
        String token = this.theSecurityService.login(theNewUser);

        if (token != null) {
            theResponse.put("token", token);
        } else {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return theResponse;
        }

        return theResponse;
    }

    @PostMapping("google")
    public HashMap<String, Object> googleLogin(@RequestBody GoogleLoginRequest request,
                                               final HttpServletResponse response) throws IOException {

        AuthResponse authResponse = googleAuthService.loginWithGoogle(request.getCredential());

        if (authResponse == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token de Google inválido");
            return new HashMap<>();
        }

        HashMap<String, Object> userResponse = new HashMap<>();
        userResponse.put("id", authResponse.getUser().getId());
        userResponse.put("name", authResponse.getUser().getName());
        userResponse.put("email", authResponse.getUser().getEmail());
        userResponse.put("authProvider", authResponse.getUser().getAuthProvider());
        userResponse.put("providerId", authResponse.getUser().getProviderId());
        userResponse.put("picture", authResponse.getUser().getPicture());
        userResponse.put("emailVerified", authResponse.getUser().getEmailVerified());
        userResponse.put("active", authResponse.getUser().getActive());

        HashMap<String, Object> theResponse = new HashMap<>();
        theResponse.put("token", authResponse.getToken());
        theResponse.put("user", userResponse);
        theResponse.put("isNewUser", authResponse.isNewUser());
        theResponse.put("requiresAdditionalInfo", authResponse.isRequiresAdditionalInfo());

        return theResponse;
    }
}