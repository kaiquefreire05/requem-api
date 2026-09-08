package com.requiem.api.service;

import com.requiem.api.dto.auth.AuthResponse;
import com.requiem.api.dto.auth.LoginRequest;
import com.requiem.api.dto.auth.RegisterRequest;
import com.requiem.api.entity.User;
import com.requiem.api.repository.UserRepository;
import com.requiem.api.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email já cadastrado");
        }

        User user = User.builder()
                .email(req.email())
                .name(req.name())
                .password(passwordEncoder.encode(req.password()))
                .build();

        user = userRepository.save(user);
        String token = jwtService.generateToken(user.getId());
        return new AuthResponse(token, new AuthResponse.UserDto(user.getId(), user.getEmail(), user.getName()));
    }

    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais inválidas"));

        if (!passwordEncoder.matches(req.password(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais inválidas");
        }

        String token = jwtService.generateToken(user.getId());
        return new AuthResponse(token, new AuthResponse.UserDto(user.getId(), user.getEmail(), user.getName()));
    }

    public AuthResponse.UserDetailDto me(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));

        return new AuthResponse.UserDetailDto(user.getId(), user.getEmail(), user.getName(), user.getCreatedAt());
    }
}
