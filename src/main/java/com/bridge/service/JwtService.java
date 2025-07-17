package com.bridge.service;

import com.bridge.model.UserPrincipal;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Service for JWT token generation, validation, and parsing
 */
@Service
public class JwtService {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);
    
    private final SecretKey secretKey;
    private final long accessTokenExpirationMinutes;
    private final long refreshTokenExpirationDays;
    private final String issuer;

    public JwtService(
            @Value("${jwt.secret:defaultSecretKeyThatShouldBeReplacedInProduction}") String secret,
            @Value("${jwt.access-token-expiration-minutes:15}") long accessTokenExpirationMinutes,
            @Value("${jwt.refresh-token-expiration-days:7}") long refreshTokenExpirationDays,
            @Value("${jwt.issuer:fhir-bridge}") String issuer) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.accessTokenExpirationMinutes = accessTokenExpirationMinutes;
        this.refreshTokenExpirationDays = refreshTokenExpirationDays;
        this.issuer = issuer;
    }

    /**
     * Generate access token for authenticated user
     */
    public String generateAccessToken(UserPrincipal userPrincipal) {
        Instant now = Instant.now();
        Instant expiration = now.plus(accessTokenExpirationMinutes, ChronoUnit.MINUTES);

        return Jwts.builder()
                .subject(userPrincipal.getUserId())
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .claim("username", userPrincipal.getUsername())
                .claim("organizationId", userPrincipal.getOrganizationId())
                .claim("roles", userPrincipal.getRoles())
                .claim("type", "access")
                .signWith(secretKey)
                .compact();
    }

    /**
     * Generate refresh token for user
     */
    public String generateRefreshToken(String userId) {
        Instant now = Instant.now();
        Instant expiration = now.plus(refreshTokenExpirationDays, ChronoUnit.DAYS);

        return Jwts.builder()
                .subject(userId)
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .claim("type", "refresh")
                .signWith(secretKey)
                .compact();
    }

    /**
     * Validate JWT token and return claims if valid
     */
    public Claims validateToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .requireIssuer(issuer)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            logger.warn("JWT token expired: {}", e.getMessage());
            throw new JwtException("Token expired", e);
        } catch (UnsupportedJwtException e) {
            logger.warn("Unsupported JWT token: {}", e.getMessage());
            throw new JwtException("Unsupported token", e);
        } catch (MalformedJwtException e) {
            logger.warn("Malformed JWT token: {}", e.getMessage());
            throw new JwtException("Malformed token", e);
        } catch (SecurityException e) {
            logger.warn("Invalid JWT signature: {}", e.getMessage());
            throw new JwtException("Invalid signature", e);
        } catch (IllegalArgumentException e) {
            logger.warn("JWT token compact of handler are invalid: {}", e.getMessage());
            throw new JwtException("Invalid token", e);
        }
    }

    /**
     * Extract user principal from JWT claims
     */
    public UserPrincipal extractUserPrincipal(Claims claims) {
        String userId = claims.getSubject();
        String username = claims.get("username", String.class);
        String organizationId = claims.get("organizationId", String.class);
        
        @SuppressWarnings("unchecked")
        List<String> roles = claims.get("roles", List.class);
        
        return new UserPrincipal(userId, username, organizationId, roles, true);
    }

    /**
     * Check if token is access token
     */
    public boolean isAccessToken(Claims claims) {
        return "access".equals(claims.get("type", String.class));
    }

    /**
     * Check if token is refresh token
     */
    public boolean isRefreshToken(Claims claims) {
        return "refresh".equals(claims.get("type", String.class));
    }

    /**
     * Extract token from Authorization header
     */
    public String extractTokenFromHeader(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }
        return null;
    }
}