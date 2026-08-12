package com.quini6.analytics.controller;

import com.quini6.analytics.dto.LoginRequest;
import com.quini6.analytics.dto.LoginResponse;
import com.quini6.analytics.domain.entity.Usuario;
import com.quini6.analytics.domain.repository.UsuarioRepository;
import com.quini6.analytics.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final long jwtExpirationMs;

    public AuthController(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider tokenProvider,
            @Value("${jwt.expiration-ms}") long jwtExpirationMs) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.jwtExpirationMs = jwtExpirationMs;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        return usuarioRepository.findByUsername(request.username())
            .filter(u -> u.getEnabled() && passwordEncoder.matches(request.password(), u.getPasswordHash()))
            .map(u -> {
                String token = tokenProvider.generateToken(u.getUsername(), u.getRole());
                return ResponseEntity.ok(new LoginResponse(
                    token, u.getUsername(), u.getRole(), jwtExpirationMs
                ));
            })
            .orElse(ResponseEntity.status(401).build());
    }
}
