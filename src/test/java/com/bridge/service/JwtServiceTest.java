package com.bridge.service;

import com.bridge.model.UserPrincipal;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private UserPrincipal testUser;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(
                "testSecretKeyForJwtTokenGenerationAndValidation",
                15, // 15 minutes for access token
                7,  // 7 days for refresh token
                "test-issuer"
        );
        
        testUser = new UserPrincipal(
                "user123",
                "testuser",
                "org456",
                Arrays.asList("USER", "ADMIN"),
                true
        );
    }

    @Test
    void generateAccessToken_ShouldCreateValidToken() {
        // When
        String token = jwtService.generateAccessToken(testUser);
        
        // Then
        assertNotNull(token);
        assertFalse(token.isEmpty());
        
        // Validate the token
        Claims claims = jwtService.validateToken(token);
        assertEquals("user123", claims.getSubject());
        assertEquals("test-issuer", claims.getIssuer());
        assertEquals("testuser", claims.get("username"));
        assertEquals("org456", claims.get("organizationId"));
        assertEquals("access", claims.get("type"));
        
        @SuppressWarnings("unchecked")
        List<String> roles = claims.get("roles", List.class);
        assertEquals(Arrays.asList("USER", "ADMIN"), roles);
    }

    @Test
    void generateRefreshToken_ShouldCreateValidToken() {
        // When
        String token = jwtService.generateRefreshToken("user123");
        
        // Then
        assertNotNull(token);
        assertFalse(token.isEmpty());
        
        // Validate the token
        Claims claims = jwtService.validateToken(token);
        assertEquals("user123", claims.getSubject());
        assertEquals("test-issuer", claims.getIssuer());
        assertEquals("refresh", claims.get("type"));
    }

    @Test
    void validateToken_WithValidToken_ShouldReturnClaims() {
        // Given
        String token = jwtService.generateAccessToken(testUser);
        
        // When
        Claims claims = jwtService.validateToken(token);
        
        // Then
        assertNotNull(claims);
        assertEquals("user123", claims.getSubject());
        assertEquals("testuser", claims.get("username"));
    }

    @Test
    void validateToken_WithInvalidToken_ShouldThrowException() {
        // Given
        String invalidToken = "invalid.jwt.token";
        
        // When & Then
        assertThrows(JwtException.class, () -> jwtService.validateToken(invalidToken));
    }

    @Test
    void extractUserPrincipal_ShouldCreateCorrectUserPrincipal() {
        // Given
        String token = jwtService.generateAccessToken(testUser);
        Claims claims = jwtService.validateToken(token);
        
        // When
        UserPrincipal extractedUser = jwtService.extractUserPrincipal(claims);
        
        // Then
        assertEquals("user123", extractedUser.getUserId());
        assertEquals("testuser", extractedUser.getUsername());
        assertEquals("org456", extractedUser.getOrganizationId());
        assertEquals(Arrays.asList("USER", "ADMIN"), extractedUser.getRoles());
        assertTrue(extractedUser.isEnabled());
    }

    @Test
    void isAccessToken_WithAccessToken_ShouldReturnTrue() {
        // Given
        String token = jwtService.generateAccessToken(testUser);
        Claims claims = jwtService.validateToken(token);
        
        // When & Then
        assertTrue(jwtService.isAccessToken(claims));
        assertFalse(jwtService.isRefreshToken(claims));
    }

    @Test
    void isRefreshToken_WithRefreshToken_ShouldReturnTrue() {
        // Given
        String token = jwtService.generateRefreshToken("user123");
        Claims claims = jwtService.validateToken(token);
        
        // When & Then
        assertTrue(jwtService.isRefreshToken(claims));
        assertFalse(jwtService.isAccessToken(claims));
    }

    @Test
    void extractTokenFromHeader_WithValidHeader_ShouldReturnToken() {
        // Given
        String authHeader = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9";
        
        // When
        String token = jwtService.extractTokenFromHeader(authHeader);
        
        // Then
        assertEquals("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9", token);
    }

    @Test
    void extractTokenFromHeader_WithInvalidHeader_ShouldReturnNull() {
        // Given
        String authHeader = "Invalid header";
        
        // When
        String token = jwtService.extractTokenFromHeader(authHeader);
        
        // Then
        assertNull(token);
    }

    @Test
    void extractTokenFromHeader_WithNullHeader_ShouldReturnNull() {
        // When
        String token = jwtService.extractTokenFromHeader(null);
        
        // Then
        assertNull(token);
    }
}