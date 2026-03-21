package com.clinica.users.infrastructure.security;

import com.clinica.users.domain.entities.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Custom implementation of Spring Security's UserDetails.
 * Wraps our domain User entity to adapt it to Spring Security mechanism.
 */
public class CustomUserDetails implements UserDetails {

    private final User user;

    public CustomUserDetails(User user) {
        this.user = user;
    }

    /**
     * Converts the UserRole enum into a GrantedAuthority.
     * Prefixes with "ROLE_" to allow usage of hasRole() in method security or matchers.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    /**
     * The account is not expired.
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * The account is not locked.
     */
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /**
     * The credentials are not expired.
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * Maps to our internal enabled flag (soft delete or suspension).
     */
    @Override
    public boolean isEnabled() {
        return user.getEnabled();
    }
    
    /**
     * Exposes the underlying internal ID if needed.
     */
    public Long getId() {
        return user.getId();
    }
}
