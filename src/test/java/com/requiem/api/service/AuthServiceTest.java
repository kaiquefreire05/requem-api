package com.requiem.api.service;

import com.requiem.api.dto.auth.AuthResponse;
import com.requiem.api.dto.auth.LoginRequest;
import com.requiem.api.dto.auth.RegisterRequest;
import com.requiem.api.entity.User;
import com.requiem.api.repository.UserRepository;
import com.requiem.api.security.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService")
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;

    @InjectMocks AuthService authService;

    // ── Fixtures ────────────────────────────────────────────────────────────

    private User buildUser() {
        return User.builder()
                .id("user-123")
                .email("test@example.com")
                .name("Test User")
                .password("$2a$10$hashedPassword")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    // ── register ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("register: success returns token + user dto")
    void register_success() {
        var req = new RegisterRequest("test@example.com", "Test User", "password123");
        User saved = buildUser();

        when(userRepository.existsByEmail(req.email())).thenReturn(false);
        when(passwordEncoder.encode(req.password())).thenReturn("$2a$10$hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(saved);
        when(jwtService.generateToken("user-123")).thenReturn("jwt-token");

        AuthResponse result = authService.register(req);

        assertThat(result.token()).isEqualTo("jwt-token");
        assertThat(result.user().id()).isEqualTo("user-123");
        assertThat(result.user().email()).isEqualTo("test@example.com");
        assertThat(result.user().name()).isEqualTo("Test User");

        verify(userRepository).save(any(User.class));
        verify(passwordEncoder).encode("password123");
    }

    @Test
    @DisplayName("register: duplicate email throws 409")
    void register_emailAlreadyExists() {
        var req = new RegisterRequest("taken@example.com", "User", "password123");
        when(userRepository.existsByEmail(req.email())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    var rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode().value()).isEqualTo(409);
                    assertThat(rse.getReason()).contains("Email já cadastrado");
                });

        verify(userRepository, never()).save(any());
    }

    // ── login ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("login: success returns token + user dto")
    void login_success() {
        var req = new LoginRequest("test@example.com", "password123");
        User user = buildUser();

        when(userRepository.findByEmail(req.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", user.getPassword())).thenReturn(true);
        when(jwtService.generateToken("user-123")).thenReturn("jwt-token");

        AuthResponse result = authService.login(req);

        assertThat(result.token()).isEqualTo("jwt-token");
        assertThat(result.user().email()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("login: user not found throws 401")
    void login_userNotFound() {
        var req = new LoginRequest("nobody@example.com", "password123");
        when(userRepository.findByEmail(req.email())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    @DisplayName("login: wrong password throws 401")
    void login_wrongPassword() {
        var req = new LoginRequest("test@example.com", "wrongpass");
        User user = buildUser();

        when(userRepository.findByEmail(req.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpass", user.getPassword())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    // ── me ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("me: returns user detail dto")
    void me_success() {
        User user = buildUser();
        when(userRepository.findById("user-123")).thenReturn(Optional.of(user));

        AuthResponse.UserDetailDto dto = authService.me("user-123");

        assertThat(dto.id()).isEqualTo("user-123");
        assertThat(dto.email()).isEqualTo("test@example.com");
        assertThat(dto.name()).isEqualTo("Test User");
        assertThat(dto.createdAt()).isNotNull();
    }

    @Test
    @DisplayName("me: user not found throws 404")
    void me_userNotFound() {
        when(userRepository.findById("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.me("ghost"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }
}
