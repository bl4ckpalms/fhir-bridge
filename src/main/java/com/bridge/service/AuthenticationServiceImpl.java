package com.bridge.service;

import com.bridge.model.UserPrincipal;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of AuthenticationService with OAuth2 and JWT support
 */
@Service
public class AuthenticationServiceImpl implements AuthenticationService {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationServiceImpl.class);
    
    private final JwtService jwtService;
    private final RestTemplate restTemplate;
    private final Set<String> revokedTokens = ConcurrentHashMap.newKeySet();
    
    // OAuth2 configuration
    private final String oauth2TokenEndpoint;
    private final String oauth2ClientId;
    private final String oauth2ClientSecret;
    private final String oauth2UserInfoEndpoint;

    public AuthenticationServiceImpl(
            JwtService jwtService,
            @Value("${oauth2.token-endpoint:https://oauth2.example.com/token}") String oauth2TokenEndpoint,
            @Value("${oauth2.client-id:fhir-bridge-client}") String oauth2ClientId,
            @Value("${oauth2.client-secret:client-secret}") String oauth2ClientSecret,
            @Value("${oauth2.userinfo-endpoint:https://oauth2.example.com/userinfo}") String oauth2UserInfoEndpoint) {
        this.jwtService = jwtService;
        this.restTemplate = new RestTemplate();
        this.oauth2TokenEndpoint = oauth2TokenEndpoint;
        this.oauth2ClientId = oauth2ClientId;
        this.oauth2ClientSecret = oauth2ClientSecret;
        this.oauth2UserInfoEndpoint = oauth2UserInfoEndpoint;
    }

    @Override
    public UserPrincipal authenticateWithOAuth2(String authorizationCode, String redirectUri) {
        try {
            // Exchange authorization code for access token
            String accessToken = exchangeCodeForToken(authorizationCode, redirectUri);
            
            // Get user info from OAuth2 provider
            Map<String, Object> userInfo = getUserInfo(accessToken);
            
            // Create UserPrincipal from OAuth2 user info
            return createUserPrincipalFromOAuth2(userInfo);
            
        } catch (Exception e) {
            logger.error("OAuth2 authentication failed", e);
            throw new RuntimeException("OAuth2 authentication failed", e);
        }
    }

    @Override
    public UserPrincipal authenticateWithClientCredentials(String clientId, String clientSecret) {
        try {
            // Validate client credentials (in real implementation, check against database)
            if (!isValidClient(clientId, clientSecret)) {
                throw new RuntimeException("Invalid client credentials");
            }
            
            // Create system user principal for client credentials flow
            return new UserPrincipal(
                    clientId,
                    clientId,
                    extractOrganizationFromClientId(clientId),
                    Arrays.asList("SYSTEM", "API_ACCESS"),
                    true
            );
            
        } catch (Exception e) {
            logger.error("Client credentials authentication failed", e);
            throw new RuntimeException("Client credentials authentication failed", e);
        }
    }

    @Override
    public String refreshAccessToken(String refreshToken) {
        try {
            Claims claims = jwtService.validateToken(refreshToken);
            
            if (!jwtService.isRefreshToken(claims)) {
                throw new JwtException("Invalid refresh token");
            }
            
            if (revokedTokens.contains(refreshToken)) {
                throw new JwtException("Refresh token has been revoked");
            }
            
            String userId = claims.getSubject();
            
            // In real implementation, fetch user details from database
            UserPrincipal userPrincipal = getUserPrincipalById(userId);
            
            return jwtService.generateAccessToken(userPrincipal);
            
        } catch (Exception e) {
            logger.error("Token refresh failed", e);
            throw new RuntimeException("Token refresh failed", e);
        }
    }

    @Override
    public String generateAccessToken(UserPrincipal userPrincipal) {
        return jwtService.generateAccessToken(userPrincipal);
    }

    @Override
    public String generateRefreshToken(String userId) {
        return jwtService.generateRefreshToken(userId);
    }

    @Override
    public UserPrincipal validateToken(String token) {
        if (revokedTokens.contains(token)) {
            throw new JwtException("Token has been revoked");
        }
        
        Claims claims = jwtService.validateToken(token);
        return jwtService.extractUserPrincipal(claims);
    }

    @Override
    public void revokeToken(String token) {
        revokedTokens.add(token);
        logger.info("Token revoked successfully");
    }

    private String exchangeCodeForToken(String authorizationCode, String redirectUri) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth(oauth2ClientId, oauth2ClientSecret);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("code", authorizationCode);
        body.add("redirect_uri", redirectUri);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(oauth2TokenEndpoint, request, Map.class);
        
        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            return (String) response.getBody().get("access_token");
        }
        
        throw new RuntimeException("Failed to exchange authorization code for token");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        
        HttpEntity<String> request = new HttpEntity<>(headers);
        
        ResponseEntity<Map> response = restTemplate.exchange(
                oauth2UserInfoEndpoint, HttpMethod.GET, request, Map.class);
        
        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            return response.getBody();
        }
        
        throw new RuntimeException("Failed to get user info from OAuth2 provider");
    }

    @SuppressWarnings("unchecked")
    private UserPrincipal createUserPrincipalFromOAuth2(Map<String, Object> userInfo) {
        String userId = (String) userInfo.get("sub");
        String username = (String) userInfo.get("preferred_username");
        String organizationId = (String) userInfo.get("organization_id");
        
        // Extract roles from OAuth2 user info
        List<String> roles = (List<String>) userInfo.getOrDefault("roles", Arrays.asList("USER"));
        
        return new UserPrincipal(userId, username, organizationId, roles, true);
    }

    private boolean isValidClient(String clientId, String clientSecret) {
        // In real implementation, validate against database or external service
        // For now, accept any non-empty credentials
        return clientId != null && !clientId.trim().isEmpty() && 
               clientSecret != null && !clientSecret.trim().isEmpty();
    }

    private String extractOrganizationFromClientId(String clientId) {
        // Extract organization from client ID (e.g., "org1-api-client" -> "org1")
        if (clientId.contains("-")) {
            return clientId.split("-")[0];
        }
        return "default-org";
    }

    private UserPrincipal getUserPrincipalById(String userId) {
        // In real implementation, fetch from database
        // For now, return a mock user principal
        return new UserPrincipal(
                userId,
                "user-" + userId,
                "default-org",
                Arrays.asList("USER"),
                true
        );
    }
}