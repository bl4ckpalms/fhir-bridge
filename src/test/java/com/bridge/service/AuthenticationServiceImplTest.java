package com.bridge.service;

import com.bridge.model.UserPrincipal;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceImplTest {

    @Mock
    private JwtService jwtService;

    private AuthenticationServiceImpl authenticationService;
    private UserPrincipal testUser;

    @BeforeEach
    void setUp() {
        authenticationService = new AuthenticationServiceImpl(
                jwtService,
                "https://oauth2.test.com/token",
                "test-client-id",
                "test-client-secret",
                "https://oauth2.test.com/userinfo"
        );
        
        testUser = new UserPrincipal(
                "user123",
                "testuser",
                "org456",
                Arrays.asList("USER"),
                true
        );
    }

    @Test
    void authenticateWithClientCredentials_WithValidCredentials_ShouldReturnUserPrincipal() {
        // Given
        String clientId = "valid-client";
        String clientSecret = "valid-secret";
        
        // When
        UserPrincipal result = authenticationService.authenticateWithClientCredentials(clientId, clientSecret);
        
        // Then
        assertNotNull(result);
        assertEquals(clientId, result.getUserId());
        assertEquals(clientId, result.getUsername());
        assertTrue(result.getRoles().contains("SYSTEM"));
        assertTrue(result.getRoles().contains("API_ACCESS"));
    }

    @Test
    void authenticateWithClientCredentials_WithInvalidCredentials_ShouldThrowException() {
        // Given
        String clientId = "";
        String clientSecret = "secret";
        
        // When & Then
        assertThrows(RuntimeException.class, 
                () -> authenticationService.authenticateWithClientCredentials(clientId, clientSecret));
    }

    @Test
    void generateAccessToken_ShouldDelegateToJwtService() {
        // Given
        String expectedToken = "access.jwt.token";
        when(jwtService.generateAccessToken(testUser)).thenReturn(expectedToken);
        
        // When
        String result = authenticationService.generateAccessToken(testUser);
        
        // Then
        assertEquals(expectedToken, result);
        verify(jwtService).generateAccessToken(testUser);
    }

    @Test
    void generateRefreshToken_ShouldDelegateToJwtService() {
        // Given
        String userId = "user123";
        String expectedToken = "refresh.jwt.token";
        when(jwtService.generateRefreshToken(userId)).thenReturn(expectedToken);
        
        // When
        String result = authenticationService.generateRefreshToken(userId);
        
        // Then
        assertEquals(expectedToken, result);
        verify(jwtService).generateRefreshToken(userId);
    }

    @Test
    void validateToken_WithValidToken_ShouldReturnUserPrincipal() {
        // Given
        String token = "valid.jwt.token";
        when(jwtService.validateToken(token)).thenReturn(null); // Claims not needed for this test
        when(jwtService.extractUserPrincipal(any())).thenReturn(testUser);
        
        // When
        UserPrincipal result = authenticationService.validateToken(token);
        
        // Then
        assertEquals(testUser, result);
        verify(jwtService).validateToken(token);
    }

    @Test
    void validateToken_WithRevokedToken_ShouldThrowException() {
        // Given
        String token = "revoked.jwt.token";
        authenticationService.revokeToken(token);
        
        // When & Then
        assertThrows(JwtException.class, () -> authenticationService.validateToken(token));
    }

    @Test
    void revokeToken_ShouldAddTokenToRevokedSet() {
        // Given
        String token = "token.to.revoke";
        
        // When
        authenticationService.revokeToken(token);
        
        // Then
        // Verify token is revoked by trying to validate it
        assertThrows(JwtException.class, () -> authenticationService.validateToken(token));
    }
}