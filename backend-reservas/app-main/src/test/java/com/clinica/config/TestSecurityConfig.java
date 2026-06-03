package com.clinica.config;

/**
 * Configuración de seguridad exclusiva para tests.
 *
 * Problema que resuelve:
 *   @MockBean JwtDecoder falla porque Spring Security intenta conectarse
 *   a Keycloak al arrancar para auto-descubrir el issuer-uri.
 *
 * Solución:
 *   Proporcionamos un JwtDecoder real pero FALSO que acepta cualquier token
 *   y devuelve siempre un JWT válido con todos los roles.
 *   No necesita red, no necesita Keycloak.
 *
 * También mockeamos JavaMailSender para evitar errores de conexión SMTP.
 */

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;

@TestConfiguration
public class TestSecurityConfig {

    /**
     * JwtDecoder de prueba.
     * Acepta CUALQUIER token y lo convierte en un JWT con todos los roles.
     * @Primary hace que Spring use este bean en lugar del real.
     */
    @Bean
    @Primary
    public JwtDecoder testJwtDecoder() {
        return token -> Jwt.withTokenValue(token)
                .header("alg", "RS256")
                // El token tiene todos los roles → cada test puede usar cualquier endpoint
                .claim("realm_access", Map.of("roles",
                        List.of("PATIENT", "ADMIN", "DOCTOR", "SCHEDULER")))
                .claim("preferred_username", "test-user")
                .claim("given_name",  "Test")
                .claim("family_name", "User")
                .subject("test-user")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }

    /**
     * JavaMailSender mock.
     * Evita que Spring intente conectarse a un servidor SMTP real durante los tests.
     */
    @Bean
    @Primary
    public JavaMailSender testMailSender() {
        return mock(JavaMailSender.class);
    }
}
