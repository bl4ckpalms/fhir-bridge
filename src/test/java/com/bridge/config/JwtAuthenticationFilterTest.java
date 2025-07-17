package com.bridge.config;

import com.bridge.model.UserPrincipal;
import com.bridge.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter jwtAuthenticationFilter;
    private UserPrincipal testUser;

    @BeforeEach
    void setUp() {
        jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtService);
        testUser = new UserPrincipal(
                "user123",
                "testuser",
                "org456",
                Arrays.asList("USER"),
                true
        );
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_WithValidJwtToken_ShouldSetAuthentication() throws ServletException, IOException {
        // Given
        String authHeader = "Bearer valid.jwt.token";
        String token = "valid.jwt.token";
        Claims claims = mock(Claims.class);
        
        when(request.getHeader("Authorization")).thenReturn(authHeader);
        when(jwtService.extractTokenFromHeader(authHeader)).thenReturn(token);
        when(jwtService.validateToken(token)).thenReturn(claims);
        when(jwtService.isAccessToken(claims)).thenReturn(true);
        when(jwtService.extractUserPrincipal(claims)).thenReturn(testUser);
        
        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);
        
        // Then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertTrue(authentication.isAuthenticated());
        assertEquals(testUser, authentication.getPrincipal());
        
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_WithInvalidToken_ShouldClearAuthentication() throws ServletException, IOException {
        // Given
        String authHeader = "Bearer invalid.jwt.token";
        String token = "invalid.jwt.token";
        
        when(request.getHeader("Authorization")).thenReturn(authHeader);
        when(jwtService.extractTokenFromHeader(authHeader)).thenReturn(token);
        when(jwtService.validateToken(token)).thenThrow(new JwtException("Invalid token"));
        
        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);
        
        // Then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNull(authentication);
        
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_WithRefreshToken_ShouldNotSetAuthentication() throws ServletException, IOException {
        // Given
        String authHeader = "Bearer refresh.jwt.token";
        String token = "refresh.jwt.token";
        Claims claims = mock(Claims.class);
        
        when(request.getHeader("Authorization")).thenReturn(authHeader);
        when(jwtService.extractTokenFromHeader(authHeader)).thenReturn(token);
        when(jwtService.validateToken(token)).thenReturn(claims);
        when(jwtService.isAccessToken(claims)).thenReturn(false);
        
        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);
        
        // Then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNull(authentication);
        
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_WithoutAuthorizationHeader_ShouldContinueFilter() throws ServletException, IOException {
        // Given
        when(request.getHeader("Authorization")).thenReturn(null);
        
        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);
        
        // Then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNull(authentication);
        
        verify(filterChain).doFilter(request, response);
        verify(jwtService, never()).validateToken(anyString());
    }

    @Test
    void doFilterInternal_WithInvalidAuthorizationHeader_ShouldContinueFilter() throws ServletException, IOException {
        // Given
        when(request.getHeader("Authorization")).thenReturn("Basic dXNlcjpwYXNz");
        
        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);
        
        // Then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNull(authentication);
        
        verify(filterChain).doFilter(request, response);
        verify(jwtService, never()).validateToken(anyString());
    }
}