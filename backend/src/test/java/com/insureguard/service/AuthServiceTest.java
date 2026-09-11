package com.insureguard.service;

import com.insureguard.dto.AuthResponse;
import com.insureguard.dto.LoginRequest;
import com.insureguard.dto.RegisterRequest;
import com.insureguard.entity.Role;
import com.insureguard.entity.User;
import com.insureguard.exception.DuplicateResourceException;
import com.insureguard.exception.InvalidCredentialsException;
import com.insureguard.repository.UserRepository;
import com.insureguard.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest("Matrixx", "matrixx@example.com", "password123", Role.USER);
    }

    @Test
    void register_throwsWhenEmailAlreadyExists() {
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> authService.register(registerRequest));
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_savesUserAndReturnsToken() {
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed-password");

        User savedUser = User.builder()
                .id(1L)
                .name(registerRequest.getName())
                .email(registerRequest.getEmail())
                .password("hashed-password")
                .role(Role.USER)
                .build();
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtUtil.generateToken(anyString(), anyString(), any())).thenReturn("fake-jwt-token");

        AuthResponse response = authService.register(registerRequest);

        assertEquals("fake-jwt-token", response.getToken());
        assertEquals(registerRequest.getEmail(), response.getEmail());
        assertEquals("USER", response.getRole());
    }

    @Test
    void login_throwsWhenUserNotFound() {
        LoginRequest loginRequest = new LoginRequest("missing@example.com", "password123");
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class, () -> authService.login(loginRequest));
    }

    @Test
    void login_throwsWhenPasswordDoesNotMatch() {
        LoginRequest loginRequest = new LoginRequest("matrixx@example.com", "wrongpassword");
        User existingUser = User.builder()
                .id(1L).name("Matrixx").email("matrixx@example.com")
                .password("hashed-password").role(Role.USER).build();

        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches(loginRequest.getPassword(), existingUser.getPassword())).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> authService.login(loginRequest));
    }

    @Test
    void login_succeedsAndReturnsToken() {
        LoginRequest loginRequest = new LoginRequest("matrixx@example.com", "password123");
        User existingUser = User.builder()
                .id(1L).name("Matrixx").email("matrixx@example.com")
                .password("hashed-password").role(Role.USER).build();

        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches(loginRequest.getPassword(), existingUser.getPassword())).thenReturn(true);
        when(jwtUtil.generateToken(anyString(), anyString(), any())).thenReturn("fake-jwt-token");

        AuthResponse response = authService.login(loginRequest);

        assertEquals("fake-jwt-token", response.getToken());
        assertEquals("matrixx@example.com", response.getEmail());
    }
}
