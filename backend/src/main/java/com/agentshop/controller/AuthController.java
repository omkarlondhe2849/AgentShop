package com.agentshop.controller;

import com.agentshop.model.User;
import com.agentshop.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final UserRepository userRepository;

    public AuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public record AuthRequest(String email, String password, String name) {}

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AuthRequest request) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email already registered"));
        }

        User user = new User(request.email(), hashPassword(request.password()), request.name());
        userRepository.save(user);

        // Simple token generation for demo purposes
        String token = UUID.randomUUID().toString();
        
        return ResponseEntity.ok(Map.of(
            "message", "Registration successful",
            "token", token,
            "user", Map.of("id", user.getId(), "name", user.getName(), "email", user.getEmail())
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        Optional<User> userOpt = userRepository.findByEmail(request.email());
        if (userOpt.isEmpty() || !userOpt.get().getPassword().equals(hashPassword(request.password()))) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid email or password"));
        }

        User user = userOpt.get();
        String token = UUID.randomUUID().toString();

        return ResponseEntity.ok(Map.of(
            "message", "Login successful",
            "token", token,
            "user", Map.of("id", user.getId(), "name", user.getName(), "email", user.getEmail())
        ));
    }

    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Could not hash password", e);
        }
    }
}
