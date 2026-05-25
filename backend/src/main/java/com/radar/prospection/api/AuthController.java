package com.radar.prospection.api;

import com.radar.prospection.domain.User;
import com.radar.prospection.repository.UserRepository;
import com.radar.prospection.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authManager;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String name  = body.get("name");
        String pass  = body.get("password");

        if (email == null || pass == null || name == null)
            return ResponseEntity.badRequest().body(Map.of("error", "Champs requis manquants"));

        if (userRepository.existsByEmail(email))
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "Cet email est déjà utilisé"));

        User user = userRepository.save(User.builder()
            .email(email)
            .name(name)
            .password(passwordEncoder.encode(pass))
            .build());

        return ResponseEntity.ok(Map.of(
            "token", jwtService.generate(user),
            "name",  user.getName(),
            "email", user.getEmail()
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String pass  = body.get("password");

        try {
            authManager.authenticate(new UsernamePasswordAuthenticationToken(email, pass));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Email ou mot de passe incorrect"));
        }

        User user = userRepository.findByEmail(email).orElseThrow();
        return ResponseEntity.ok(Map.of(
            "token", jwtService.generate(user),
            "name",  user.getName(),
            "email", user.getEmail()
        ));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(@RequestHeader("Authorization") String header) {
        String token = header.substring(7);
        String email = jwtService.extractEmail(token);
        return userRepository.findByEmail(email)
            .map(u -> ResponseEntity.ok(Map.of("name", u.getName(), "email", u.getEmail())))
            .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }
}
