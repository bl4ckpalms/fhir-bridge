package com.bridge.config;

import com.bridge.model.JwtAuthenticationToken;
import com.bridge.model.UserPrincipal;
import com.bridge.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT authentication filter that validates JWT tokens and sets authentication context
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    
    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        String authorizationHeader = request.getHeader("Authorization");
        
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = jwtService.extractTokenFromHeader(authorizationHeader);
            if (token != null) {
                Claims claims = jwtService.validateToken(token);
                
                // Only process access tokens for authentication
                if (jwtService.isAccessToken(claims)) {
                    UserPrincipal userPrincipal = jwtService.extractUserPrincipal(claims);
                    JwtAuthenticationToken authentication = new JwtAuthenticationToken(
                            userPrincipal, token, userPrincipal.getAuthorities());
                    
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    logger.debug("Successfully authenticated user: {}", userPrincipal.getUsername());
                } else {
                    logger.warn("Invalid token type for authentication");
                }
            }
        } catch (JwtException e) {
            logger.warn("JWT authentication failed: {}", e.getMessage());
            // Clear any existing authentication
            SecurityContextHolder.clearContext();
        } catch (Exception e) {
            logger.error("Unexpected error during JWT authentication", e);
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}