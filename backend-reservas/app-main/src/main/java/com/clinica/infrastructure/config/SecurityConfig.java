package com.clinica.infrastructure.config;

import com.clinica.users.infrastructure.security.AuthTokenFilter;
import com.clinica.users.infrastructure.security.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Main Spring Security Configuration class.
 * Configures JWT filtration, CORS, CSRF, and route-based authorization rules.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final AuthTokenFilter authTokenFilter;

    /**
     * Constructor injection for our custom implementations.
     *
     * @param userDetailsService To load the user from db.
     * @param authTokenFilter    To intercept requests and validate the JWT.
     */
    public SecurityConfig(CustomUserDetailsService userDetailsService, AuthTokenFilter authTokenFilter) {
        this.userDetailsService = userDetailsService;
        this.authTokenFilter = authTokenFilter;
    }

    /**
     * Exposes the AuthenticationManager as a bean for manual authentication (like login).
     *
     * @param authConfig Authentication configuration.
     * @return AuthenticationManager instance.
     * @throws Exception If there's an issue obtaining the manager.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    /**
     * Provides the BCryptPasswordEncoder bean used across the application to encode/verify passwords.
     *
     * @return PasswordEncoder instance.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Configures the Security Filter Chain. Maps routes to roles and ensures sessions are stateless.
     *
     * @param http the HttpSecurity configuration object.
     * @return the fully configured SecurityFilterChain.
     * @throws Exception on configuration errors.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/**").permitAll()
                        
                        // Protected endpoints for Doctors
                        .requestMatchers(HttpMethod.POST, "/api/v1/doctors").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/doctors/**/schedules").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/doctors/**").hasAnyRole("ADMIN", "SCHEDULER")
                        
                        // Protected endpoints for Patients
                        .requestMatchers(HttpMethod.POST, "/api/v1/patients").hasAnyRole("ADMIN", "SCHEDULER")
                        
                        // Protected endpoints for Appointments
                        .requestMatchers(HttpMethod.POST, "/api/v1/appointments/manual").hasAnyRole("ADMIN", "SCHEDULER")
                        
                        // Global System Configuration
                        .requestMatchers(HttpMethod.PUT, "/api/v1/configurations/**").hasRole("ADMIN")
                        
                        // Any other request must be authenticated
                        .anyRequest().authenticated()
                );

        // Add our custom JWT filter *before* the UsernamePasswordAuthenticationFilter
        http.addFilterBefore(authTokenFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
