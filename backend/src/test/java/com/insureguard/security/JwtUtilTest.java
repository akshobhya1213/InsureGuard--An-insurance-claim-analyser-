package com.insureguard.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "test-secret-key-that-is-long-enough-for-hs256-signing");
        ReflectionTestUtils.setField(jwtUtil, "expirationMs", 3600000L);
    }

    @Test
    void generatesAndParsesTokenCorrectly() {
        String token = jwtUtil.generateToken("matrixx@example.com", "USER", 42L);

        assertNotNull(token);
        assertEquals("matrixx@example.com", jwtUtil.extractEmail(token));
        assertEquals("USER", jwtUtil.extractRole(token));
        assertEquals(42L, jwtUtil.extractUserId(token));
    }

    @Test
    void isTokenValid_returnsTrueForMatchingUser() {
        String token = jwtUtil.generateToken("matrixx@example.com", "USER", 42L);
        UserDetails userDetails = new User("matrixx@example.com", "irrelevant", java.util.List.of());

        assertTrue(jwtUtil.isTokenValid(token, userDetails));
    }

    @Test
    void isTokenValid_returnsFalseForDifferentUser() {
        String token = jwtUtil.generateToken("matrixx@example.com", "USER", 42L);
        UserDetails otherUser = new User("someone-else@example.com", "irrelevant", java.util.List.of());

        assertFalse(jwtUtil.isTokenValid(token, otherUser));
    }
}
