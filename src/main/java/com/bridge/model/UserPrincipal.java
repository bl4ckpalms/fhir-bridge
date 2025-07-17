package com.bridge.model;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * User principal for JWT authentication containing user details and roles
 */
public class UserPrincipal implements UserDetails {
    
    private final String userId;
    private final String username;
    private final String organizationId;
    private final List<String> roles;
    private final boolean enabled;

    public UserPrincipal(String userId, String username, String organizationId, List<String> roles, boolean enabled) {
        this.userId = userId;
        this.username = username;
        this.organizationId = organizationId;
        this.roles = roles;
        this.enabled = enabled;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());
    }

    @Override
    public String getPassword() {
        return null; // JWT tokens don't require password
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return enabled;
    }

    @Override
    public boolean isAccountNonLocked() {
        return enabled;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return enabled;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public String getUserId() {
        return userId;
    }

    public String getOrganizationId() {
        return organizationId;
    }

    public List<String> getRoles() {
        return roles;
    }
}