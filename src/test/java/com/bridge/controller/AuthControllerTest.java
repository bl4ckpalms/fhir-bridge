package com.bridge.controller;

import com.bridge.model.UserPrincipal;
import com.bridge.service.AuthenticationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class)
@Import(AuthControllerTest.TestSecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthenticationService authenticationService;

    @Autowired
    private ObjectMapper objectMapper;
    
    @TestConfiguration
    static class TestSecurityConfig {
        @Bean
        @Primary
        public SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
            http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authz -> authz.anyRequest().permitAll());
            return http.build();
        }
    }

    @Test
    void oauth2Token_WithValidCredentials_ShouldReturnTokens() throws Exception {
        // Given
        UserPrincipal testUser = new UserPrincipal(
                "user123", "testuser", "org456", Arrays.asList("USER"), true);
        
        when(authenticationService.authenticateWithOAuth2(anyString(), anyString()))
                .thenReturn(testUser);
        when(authenticationService.generateAccessToken(any(UserPrincipal.class)))
                .thenReturn("access.token");
        when(authenticationService.generateRefreshToken(anyString()))
                .thenReturn("refresh.token");

        // When & Then
        mockMvc.perform(post("/api/v1/auth/oauth2/token")
                        .param("code", "auth_code_123")
                        .param("redirect_uri", "https://example.com/callback")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").value("access.token"))
                .andExpect(jsonPath("$.refresh_token").value("refresh.token"))
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.expires_in").value(900))
                .andExpect(jsonPath("$.user_id").value("user123"))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.organization_id").value("org456"));
    }

    @Test
    void oauth2Token_WithInvalidCredentials_ShouldReturnError() throws Exception {
        // Given
        when(authenticationService.authenticateWithOAuth2(anyString(), anyString()))
                .thenThrow(new RuntimeException("Invalid authorization code"));

        // When & Then
        mockMvc.perform(post("/api/v1/auth/oauth2/token")
                        .param("code", "invalid_code")
                        .param("redirect_uri", "https://example.com/callback")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("authentication_failed"))
                .andExpect(jsonPath("$.error_description").value("Invalid authorization code"));
    }

    @Test
    void clientCredentialsToken_WithValidCredentials_ShouldReturnToken() throws Exception {
        // Given
        UserPrincipal testClient = new UserPrincipal(
                "client123", "client123", "org456", Arrays.asList("SYSTEM"), true);
        
        when(authenticationService.authenticateWithClientCredentials(anyString(), anyString()))
                .thenReturn(testClient);
        when(authenticationService.generateAccessToken(any(UserPrincipal.class)))
                .thenReturn("access.token");

        // When & Then
        mockMvc.perform(post("/api/v1/auth/client-credentials/token")
                        .param("client_id", "client123")
                        .param("client_secret", "secret123")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").value("access.token"))
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.expires_in").value(900))
                .andExpect(jsonPath("$.client_id").value("client123"))
                .andExpect(jsonPath("$.organization_id").value("org456"));
    }

    @Test
    void refreshToken_WithValidRefreshToken_ShouldReturnNewAccessToken() throws Exception {
        // Given
        when(authenticationService.refreshAccessToken(anyString()))
                .thenReturn("new.access.token");

        // When & Then
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .param("refresh_token", "valid.refresh.token")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").value("new.access.token"))
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.expires_in").value(900));
    }

    @Test
    void refreshToken_WithInvalidRefreshToken_ShouldReturnError() throws Exception {
        // Given
        when(authenticationService.refreshAccessToken(anyString()))
                .thenThrow(new RuntimeException("Invalid refresh token"));

        // When & Then
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .param("refresh_token", "invalid.refresh.token")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("invalid_grant"))
                .andExpect(jsonPath("$.error_description").value("Invalid refresh token"));
    }

    @Test
    void validateToken_WithValidToken_ShouldReturnUserInfo() throws Exception {
        // Given
        UserPrincipal testUser = new UserPrincipal(
                "user123", "testuser", "org456", Arrays.asList("USER"), true);
        
        when(authenticationService.validateToken(anyString()))
                .thenReturn(testUser);

        // When & Then
        mockMvc.perform(post("/api/v1/auth/validate")
                        .header("Authorization", "Bearer valid.token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.user_id").value("user123"))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.organization_id").value("org456"))
                .andExpect(jsonPath("$.roles[0]").value("USER"));
    }

    @Test
    void validateToken_WithInvalidToken_ShouldReturnError() throws Exception {
        // Given
        when(authenticationService.validateToken(anyString()))
                .thenThrow(new RuntimeException("Invalid token"));

        // When & Then
        mockMvc.perform(post("/api/v1/auth/validate")
                        .header("Authorization", "Bearer invalid.token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.error").value("invalid_token"))
                .andExpect(jsonPath("$.error_description").value("Invalid token"));
    }

    @Test
    void revokeToken_WithValidToken_ShouldReturnSuccess() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/auth/revoke")
                        .header("Authorization", "Bearer valid.token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Token revoked successfully"));
    }

    @Test
    void revokeToken_WithoutAuthorizationHeader_ShouldReturnError() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/auth/revoke")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_request"))
                .andExpect(jsonPath("$.error_description").value("Missing or invalid Authorization header"));
    }
}