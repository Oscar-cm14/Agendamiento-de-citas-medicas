package com.clinica.users.infrastructure.controllers;

import com.clinica.shared.domain.exceptions.InvalidCredentialsException;
import com.clinica.shared.dto.JwtResponse;
import com.clinica.shared.dto.LoginRequest;
import com.clinica.users.infrastructure.security.CustomUserDetails;
import com.clinica.users.infrastructure.security.JwtUtils;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller responsible for managing authentication flows like login.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;

    /**
     * Constructor injection.
     *
     * @param authenticationManager Validates the user credentials globally.
     * @param jwtUtils              Generates the JWT.
     */
    public AuthController(AuthenticationManager authenticationManager, JwtUtils jwtUtils) {
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
    }

    /**
     * Authenticates a user and issues a JWT token.
     *
     * @param loginRequest The user's credentials.
     * @return A JwtResponse containing the token and user info.
     * @throws InvalidCredentialsException if authentication fails.
     */
    @PostMapping("/login")
    public ResponseEntity<JwtResponse> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            // Attempt to authenticate
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.username(), loginRequest.password())
            );

            // Set context explicitly 
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Generate JWT
            String jwt = jwtUtils.generateJwtToken(authentication);

            // Extract generic info
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

            // The authorities have ROLE_ prefix, let's grab the first one (since each user has 1 role)
            String role = userDetails.getAuthorities().stream()
                    .findFirst()
                    .map(item -> item.getAuthority().replace("ROLE_", ""))
                    .orElse("USER");

            return ResponseEntity.ok(new JwtResponse(jwt, userDetails.getId(), userDetails.getUsername(), role));

        } catch (BadCredentialsException e) {
            // Catch the Spring Security exception and throw our specific one to be handled by GlobalExceptionHandler
            throw new InvalidCredentialsException("Invalid username or password");
        }
    }
}
