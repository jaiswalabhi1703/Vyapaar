package com.orderplatform.gateway.auth;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Minimal in-memory user store with bcrypt-hashed passwords and a role per user. Kept in the
 * gateway as the "tiny piece of auth that can grow into its own service later". Seeded with a
 * normal demo user and an admin (admin sees stock levels).
 */
@Component
public class UserStore {

    private record Account(String passwordHash, String role) {
    }

    private final ConcurrentHashMap<String, Account> accounts = new ConcurrentHashMap<>();
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public UserStore() {
        seed("demo", "password", "USER");
        seed("admin", "admin123", "ADMIN");
    }

    private void seed(String username, String rawPassword, String role) {
        accounts.put(username, new Account(encoder.encode(rawPassword), role));
    }

    /** @return false if the username is already taken. New self-registered users are USERs. */
    public boolean register(String username, String rawPassword) {
        return accounts.putIfAbsent(username, new Account(encoder.encode(rawPassword), "USER")) == null;
    }

    public boolean authenticate(String username, String rawPassword) {
        Account a = accounts.get(username);
        return a != null && encoder.matches(rawPassword, a.passwordHash());
    }

    public String roleOf(String username) {
        Account a = accounts.get(username);
        return a != null ? a.role() : "USER";
    }
}
