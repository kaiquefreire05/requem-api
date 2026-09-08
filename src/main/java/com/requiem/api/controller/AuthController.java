package com.requiem.api.controller;

import com.requiem.api.dto.auth.AuthResponse;
import com.requiem.api.dto.auth.LoginRequest;
import com.requiem.api.dto.auth.RegisterRequest;
import com.requiem.api.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterRequest req) {
        AuthResponse res = authService.register(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("token", res.token(), "user", res.user()));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest req) {
        AuthResponse res = authService.login(req);
        return ResponseEntity.ok(Map.of("token", res.token(), "user", res.user()));
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(@AuthenticationPrincipal String userId) {
        AuthResponse.UserDetailDto user = authService.me(userId);
        return ResponseEntity.ok(Map.of("user", user));
    }
}
