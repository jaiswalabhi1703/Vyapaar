package com.orderplatform.gateway.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserStore users;
    private final JwtService jwt;

    public AuthController(UserStore users, JwtService jwt) {
        this.users = users;
        this.jwt = jwt;
    }

    public record Credentials(String username, String password) {
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Credentials c) {
        if (c.username() == null || c.password() == null || c.username().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "username and password required"));
        }
        if (!users.register(c.username(), c.password())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "username taken"));
        }
        String role = users.roleOf(c.username());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("token", jwt.issue(c.username(), role), "username", c.username(), "role", role));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Credentials c) {
        if (!users.authenticate(c.username(), c.password())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "invalid credentials"));
        }
        String role = users.roleOf(c.username());
        return ResponseEntity.ok(Map.of("token", jwt.issue(c.username(), role), "username", c.username(), "role", role));
    }
}
