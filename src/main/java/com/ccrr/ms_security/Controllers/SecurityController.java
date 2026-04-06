package com.ccrr.ms_security.Controllers;

import com.ccrr.ms_security.Models.User;
import com.ccrr.ms_security.Services.SecurityService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;

@CrossOrigin
@RestController
@RequestMapping("/security")
public class SecurityController {

    @Autowired
    private SecurityService theSecurityService;

    @PostMapping("login")
    public HashMap<String, Object> login(@RequestBody User theNewUser,
                                         final HttpServletResponse response) throws IOException {
        HashMap<String, Object> theResponse = this.theSecurityService.loginWith2FA(theNewUser);

        if (theResponse == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return new HashMap<>();
        }

        return theResponse;
    }

    @PostMapping("2fa/verify")
    public ResponseEntity<HashMap<String, Object>> verify2FA(@RequestBody HashMap<String, String> body) {
        String sessionId = body.get("sessionId");
        String code = body.get("code");

        HashMap<String, Object> result = this.theSecurityService.verify2FA(sessionId, code);

        if (result == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new HashMap<>());
        }

        Boolean authenticated = (Boolean) result.get("authenticated");

        if (Boolean.TRUE.equals(authenticated)) {
            return ResponseEntity.ok(result);
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
    }

    @PostMapping("2fa/resend")
    public ResponseEntity<HashMap<String, Object>> resend2FA(@RequestBody HashMap<String, String> body) {
        String sessionId = body.get("sessionId");

        HashMap<String, Object> result = this.theSecurityService.resend2FA(sessionId);

        if (result == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new HashMap<>());
        }

        return ResponseEntity.ok(result);
    }

    @PostMapping("2fa/cancel")
    public ResponseEntity<HashMap<String, Object>> cancel2FA(@RequestBody HashMap<String, String> body) {
        String sessionId = body.get("sessionId");

        HashMap<String, Object> result = this.theSecurityService.cancel2FA(sessionId);

        if (result == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new HashMap<>());
        }

        return ResponseEntity.ok(result);
    }
}