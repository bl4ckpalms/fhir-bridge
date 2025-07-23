package com.bridge.controller;

import com.bridge.model.UserPrincipal;
import com.bridge.service.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for authentication operations
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "OAuth2 authentication and token management operations")
public class AuthController {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    
    private final AuthenticationService authenticationService;

    public AuthController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    /**
     * OAuth2 authorization code flow endpoint
     */
    @Operation(
        summary = "OAuth2 authorization code token exchange",
        description = """
            Exchanges an OAuth2 authorization code for access and refresh tokens.
            This endpoint implements the OAuth2 authorization code flow for user authentication.
            """,
        tags = {"Authentication"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Authentication successful",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples = @ExampleObject(
                    name = "Successful authentication",
                    value = """
                        {
                          "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                          "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                          "token_type": "Bearer",
                          "expires_in": 900,
                          "user_id": "user-123",
                          "username": "john.doe",
                          "organization_id": "org-456"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Authentication failed",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples = @ExampleObject(
                    name = "Authentication error",
                    value = """
                        {
                          "error": "authentication_failed",
                          "error_description": "Invalid authorization code"
                        }
                        """
                )
            )
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    })
    @PostMapping(value = "/oauth2/token", 
                 consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> oauth2Token(
            @Parameter(description = "OAuth2 authorization code", required = true)
            @RequestParam("code") String authorizationCode,
            @Parameter(description = "Redirect URI used in authorization request", required = true)
            @RequestParam("redirect_uri") String redirectUri) {
        
        try {
            UserPrincipal userPrincipal = authenticationService.authenticateWithOAuth2(authorizationCode, redirectUri);
            
            String accessToken = authenticationService.generateAccessToken(userPrincipal);
            String refreshToken = authenticationService.generateRefreshToken(userPrincipal.getUserId());
            
            Map<String, Object> response = new HashMap<>();
            response.put("access_token", accessToken);
            response.put("refresh_token", refreshToken);
            response.put("token_type", "Bearer");
            response.put("expires_in", 900); // 15 minutes
            response.put("user_id", userPrincipal.getUserId());
            response.put("username", userPrincipal.getUsername());
            response.put("organization_id", userPrincipal.getOrganizationId());
            
            logger.info("OAuth2 authentication successful for user: {}", userPrincipal.getUsername());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("OAuth2 authentication failed", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "authentication_failed");
            error.put("error_description", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

    /**
     * Client credentials flow endpoint
     */
    @PostMapping("/client-credentials/token")
    public ResponseEntity<Map<String, Object>> clientCredentialsToken(
            @RequestParam("client_id") String clientId,
            @RequestParam("client_secret") String clientSecret) {
        
        try {
            UserPrincipal userPrincipal = authenticationService.authenticateWithClientCredentials(clientId, clientSecret);
            
            String accessToken = authenticationService.generateAccessToken(userPrincipal);
            
            Map<String, Object> response = new HashMap<>();
            response.put("access_token", accessToken);
            response.put("token_type", "Bearer");
            response.put("expires_in", 900); // 15 minutes
            response.put("client_id", userPrincipal.getUserId());
            response.put("organization_id", userPrincipal.getOrganizationId());
            
            logger.info("Client credentials authentication successful for client: {}", clientId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Client credentials authentication failed", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "authentication_failed");
            error.put("error_description", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

    /**
     * Refresh token endpoint
     */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refreshToken(
            @RequestParam("refresh_token") String refreshToken) {
        
        try {
            String newAccessToken = authenticationService.refreshAccessToken(refreshToken);
            
            Map<String, Object> response = new HashMap<>();
            response.put("access_token", newAccessToken);
            response.put("token_type", "Bearer");
            response.put("expires_in", 900); // 15 minutes
            
            logger.info("Token refresh successful");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Token refresh failed", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "invalid_grant");
            error.put("error_description", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

    /**
     * Token revocation endpoint (logout)
     */
    @PostMapping("/revoke")
    public ResponseEntity<Map<String, Object>> revokeToken(
            @RequestHeader("Authorization") String authorizationHeader) {
        
        try {
            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                String token = authorizationHeader.substring(7);
                authenticationService.revokeToken(token);
                
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Token revoked successfully");
                
                logger.info("Token revocation successful");
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "invalid_request");
                error.put("error_description", "Missing or invalid Authorization header");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }
            
        } catch (Exception e) {
            logger.error("Token revocation failed", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "server_error");
            error.put("error_description", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Token validation endpoint
     */
    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateToken(
            @RequestHeader("Authorization") String authorizationHeader) {
        
        try {
            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                String token = authorizationHeader.substring(7);
                UserPrincipal userPrincipal = authenticationService.validateToken(token);
                
                Map<String, Object> response = new HashMap<>();
                response.put("valid", true);
                response.put("user_id", userPrincipal.getUserId());
                response.put("username", userPrincipal.getUsername());
                response.put("organization_id", userPrincipal.getOrganizationId());
                response.put("roles", userPrincipal.getRoles());
                
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> error = new HashMap<>();
                error.put("valid", false);
                error.put("error", "invalid_request");
                error.put("error_description", "Missing or invalid Authorization header");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }
            
        } catch (Exception e) {
            logger.error("Token validation failed", e);
            Map<String, Object> error = new HashMap<>();
            error.put("valid", false);
            error.put("error", "invalid_token");
            error.put("error_description", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }
}