package com.bridge.service;

import com.bridge.model.UserPrincipal;

/**
 * Service interface for authentication operations including JWT validation
 */
public interface AuthenticationService {
    
    /**
     * Authenticate user with OAuth2 authorization code
     */
    UserPrincipal authenticateWithOAuth2(String authorizationCode, String redirectUri);
    
    /**
     * Authenticate user with client credentials
     */
    UserPrincipal authenticateWithClientCredentials(String clientId, String clientSecret);
    
    /**
     * Refresh access token using refresh token
     */
    String refreshAccessToken(String refreshToken);
    
    /**
     * Generate access token for authenticated user
     */
    String generateAccessToken(UserPrincipal userPrincipal);
    
    /**
     * Generate refresh token for user
     */
    String generateRefreshToken(String userId);
    
    /**
     * Validate JWT token and extract user principal
     */
    UserPrincipal validateToken(String token);
    
    /**
     * Revoke token (logout)
     */
    void revokeToken(String token);
}