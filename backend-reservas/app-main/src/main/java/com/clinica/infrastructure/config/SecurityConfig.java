package com.clinica.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import com.clinica.users.infrastructure.security.AuthTokenFilter;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final AuthTokenFilter authTokenFilter;
    private final com.clinica.users.infrastructure.security.CustomUserDetailsService userDetailsService;

    public SecurityConfig(AuthTokenFilter authTokenFilter,
                          com.clinica.users.infrastructure.security.CustomUserDetailsService userDetailsService) {
        this.authTokenFilter = authTokenFilter;
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public org.springframework.security.authentication.dao.DaoAuthenticationProvider authenticationProvider() {
        org.springframework.security.authentication.dao.DaoAuthenticationProvider authProvider =
                new org.springframework.security.authentication.dao.DaoAuthenticationProvider();
        authProvider.setUserDetailsService(this.userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .headers(headers -> headers
                    .frameOptions(frame -> frame.disable()))
            .sessionManagement(session -> session
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth

                // ── Públicos ──────────────────────────────────────────────
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers("/api/v1/auth/login").permitAll()
                .requestMatchers("/api/v1/patients/register").permitAll()

                // ── Solo ADMIN ────────────────────────────────────────────
                .requestMatchers(HttpMethod.POST,
                        "/api/v1/schedulers/register").hasRole("ADMIN")
                .requestMatchers("/api/v1/configurations/**").hasRole("ADMIN")

                // Configurar horario (PUT): solo ADMIN
                .requestMatchers(HttpMethod.PUT,
                        "/api/v1/doctors/schedules/**").hasRole("ADMIN")

                // Consultar horario (GET): ADMIN, SCHEDULER y PATIENT lo necesitan
                // para calcular las franjas disponibles
                .requestMatchers(HttpMethod.GET,
                        "/api/v1/doctors/schedules/**").hasAnyRole("ADMIN", "SCHEDULER", "PATIENT", "DOCTOR")

                // ── Médicos ───────────────────────────────────────────────
                .requestMatchers(HttpMethod.POST,
                        "/api/v1/doctors").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET,
                        "/api/v1/doctors").hasAnyRole("ADMIN", "SCHEDULER", "PATIENT", "DOCTOR")

                // ── Buscar paciente por cédula — ADMIN y SCHEDULER ────────
                .requestMatchers(HttpMethod.GET,
                        "/api/v1/patients/by-identification").hasAnyRole("ADMIN", "SCHEDULER", "DOCTOR")

                // ── Citas ─────────────────────────────────────────────────
                .requestMatchers(HttpMethod.POST,
                        "/api/v1/appointments").hasAnyRole("ADMIN", "SCHEDULER", "PATIENT", "DOCTOR")
                .requestMatchers(HttpMethod.GET,
                        "/api/v1/appointments").hasAnyRole("ADMIN", "SCHEDULER", "PATIENT", "DOCTOR")

                // ── Franjas disponibles ───────────────────────────────────
                .requestMatchers(HttpMethod.GET,
                        "/api/v1/appointments/slots").hasAnyRole("ADMIN", "SCHEDULER", "PATIENT", "DOCTOR")

                // ── RF5: Exportar citas a CSV ─────────────────────────────
                .requestMatchers(HttpMethod.GET,
                        "/api/v1/appointments/export").hasAnyRole("ADMIN", "SCHEDULER", "DOCTOR")

                // ── Todo lo demás requiere autenticación ──────────────────
                .anyRequest().authenticated()
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(authTokenFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:4200"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(4);
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}