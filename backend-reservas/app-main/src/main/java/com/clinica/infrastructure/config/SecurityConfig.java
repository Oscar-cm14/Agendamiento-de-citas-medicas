package com.clinica.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

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
                // Actuator: health e info públicos (Render los usa para verificar que el servicio esté vivo)
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                // Métricas solo para admins autenticados
                .requestMatchers("/actuator/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/v1/users/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/patients/register").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/schedulers/register").permitAll()

                // ── Solo ADMIN ────────────────────────────────────────────
                .requestMatchers("/api/v1/configurations/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/v1/doctors/schedules/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/v1/doctors/schedules/**")
                        .hasAnyRole("ADMIN", "SCHEDULER", "PATIENT", "DOCTOR")
                .requestMatchers(HttpMethod.POST, "/api/v1/doctors").hasRole("ADMIN")

                // ── Médicos ───────────────────────────────────────────────
                .requestMatchers(HttpMethod.GET, "/api/v1/doctors")
                        .hasAnyRole("ADMIN", "SCHEDULER", "PATIENT", "DOCTOR")
                .requestMatchers(HttpMethod.GET, "/api/v1/doctors/by-user/**")
                        .hasAnyRole("ADMIN", "DOCTOR")
                // GET /me — médico autenticado (debe ir ANTES de /{id})
                .requestMatchers(HttpMethod.GET, "/api/v1/doctors/me")
                        .hasRole("DOCTOR")
                // PUT /me — médico edita SU PROPIO perfil (debe ir ANTES de /{id})
                .requestMatchers(HttpMethod.PUT, "/api/v1/doctors/me")
                        .hasRole("DOCTOR")

                // ── Pacientes ─────────────────────────────────────────────
                .requestMatchers(HttpMethod.GET, "/api/v1/patients/by-identification")
                        .hasAnyRole("ADMIN", "SCHEDULER", "DOCTOR", "PATIENT")
                .requestMatchers(HttpMethod.GET, "/api/v1/patients/by-username")
                        .hasAnyRole("ADMIN", "SCHEDULER", "DOCTOR", "PATIENT")
                .requestMatchers(HttpMethod.GET, "/api/v1/patients/by-id/**")
                        .hasAnyRole("ADMIN", "SCHEDULER", "DOCTOR", "PATIENT")
                .requestMatchers(HttpMethod.PUT, "/api/v1/patients/**")
                        .hasAnyRole("ADMIN", "PATIENT")

                // ── Citas ─────────────────────────────────────────────────
                .requestMatchers(HttpMethod.POST, "/api/v1/appointments")
                        .hasAnyRole("ADMIN", "SCHEDULER", "PATIENT", "DOCTOR")
                .requestMatchers(HttpMethod.PUT, "/api/v1/appointments/**")
                        .hasAnyRole("ADMIN", "SCHEDULER", "DOCTOR")
                .requestMatchers(HttpMethod.PATCH, "/api/v1/appointments/**")
                        .hasAnyRole("ADMIN", "SCHEDULER", "PATIENT", "DOCTOR")
                .requestMatchers(HttpMethod.GET, "/api/v1/appointments")
                        .hasAnyRole("ADMIN", "SCHEDULER", "PATIENT", "DOCTOR")

                // Franjas disponibles
                .requestMatchers(HttpMethod.GET, "/api/v1/appointments/slots")
                        .hasAnyRole("ADMIN", "SCHEDULER", "PATIENT", "DOCTOR")

                // Citas prioritarias — NUEVO
                .requestMatchers(HttpMethod.GET, "/api/v1/appointments/priority")
                        .hasAnyRole("ADMIN", "SCHEDULER", "DOCTOR")

                // Exportar CSV
                .requestMatchers(HttpMethod.GET, "/api/v1/appointments/export")
                        .hasAnyRole("ADMIN", "SCHEDULER", "DOCTOR")

                // ── Gestión de usuarios (solo ADMIN) ──────────────────────
                .requestMatchers(HttpMethod.GET, "/api/v1/users").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/v1/users/*/roles").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/v1/users/*/roles").hasRole("ADMIN")

                // ── Médico: edición de perfil ─────────────────────────────
                // Solo ADMIN puede editar OTROS médicos por ID
                .requestMatchers(HttpMethod.PUT, "/api/v1/doctors/*").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/v1/doctors/*")
                        .hasAnyRole("ADMIN", "DOCTOR", "PATIENT", "SCHEDULER")

                // ── Agendadores ───────────────────────────────────────────
                .requestMatchers(HttpMethod.GET, "/api/v1/schedulers")
                        .hasRole("ADMIN")
                // GET /schedulers/me — agendador ve su propio perfil
                .requestMatchers(HttpMethod.GET, "/api/v1/schedulers/me")
                        .hasAnyRole("ADMIN", "SCHEDULER")
                // PUT /schedulers/me — agendador edita su propio perfil
                .requestMatchers(HttpMethod.PUT, "/api/v1/schedulers/me")
                        .hasAnyRole("ADMIN", "SCHEDULER")
                // GET/PUT por ID — solo ADMIN
                .requestMatchers(HttpMethod.GET, "/api/v1/schedulers/*")
                        .hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/v1/schedulers/*")
                        .hasRole("ADMIN")

                // ── Todo lo demás requiere autenticación ──────────────────
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
            );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:4200"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter() {
        JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(new KeycloakRealmRoleConverter());
        return jwtConverter;
    }

    static class KeycloakRealmRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
        @Override
        @SuppressWarnings("unchecked")
        public Collection<GrantedAuthority> convert(Jwt jwt) {
            Map<String, Object> realmAccess = (Map<String, Object>) jwt.getClaims().get("realm_access");

            if (realmAccess == null || realmAccess.isEmpty()) {
                return Collections.emptyList();
            }

            Collection<String> roles = (Collection<String>) realmAccess.get("roles");
            if (roles == null) {
                return Collections.emptyList();
            }

            return roles.stream()
                    .map(roleName -> "ROLE_" + roleName)
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
        }
    }

    @Bean
    public org.springframework.security.crypto.password.PasswordEncoder passwordEncoder() {
        return new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder(4);
    }

    @Bean
    public org.springframework.security.authentication.AuthenticationManager authenticationManager(
            org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }
}