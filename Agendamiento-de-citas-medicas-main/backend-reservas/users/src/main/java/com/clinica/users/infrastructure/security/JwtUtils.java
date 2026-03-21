package com.clinica.users.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * Utility class for generating and validating JSON Web Tokens (JWT).
 * Utilizes the io.jsonwebtoken library (JJWT 0.12+).
 */
@Component
public class JwtUtils {

    @Value("${clinica.app.jwtSecret:myVerySecretKeyForClinicaAppThatMustBeLongEnough123456}")
    private String jwtSecret;

    @Value("${clinica.app.jwtExpirationMs:86400000}")
    private int jwtExpirationMs; // Default: 24 hours

    /**
     * Generates a JWT token based on the currently authenticated user.
     *
     * @param authentication The Spring Security authentication object.
     * @return A signed JWT token containing the username and expiration logic.
     */
    public String generateJwtToken(Authentication authentication) {
        UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();

        return Jwts.builder()
                .subject(userPrincipal.getUsername())
                .issuedAt(new Date())
                .expiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(key())
                .compact();
    }

    /**
     * Validates the structure, signature, and expiration of a JWT token.
     *
     * @param authToken The raw JWT string.
     * @return true if the token is valid, false otherwise.
     */
    public boolean validateJwtToken(String authToken) {
        try {
            Jwts.parser().verifyWith(key()).build().parseSignedClaims(authToken);
            return true;
        } catch (SignatureException e) {
            System.err.println("Invalid JWT signature: " + e.getMessage());
        } catch (MalformedJwtException e) {
            System.err.println("Invalid JWT token: " + e.getMessage());
        } catch (ExpiredJwtException e) {
            System.err.println("JWT token is expired: " + e.getMessage());
        } catch (UnsupportedJwtException e) {
            System.err.println("JWT token is unsupported: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println("JWT claims string is empty: " + e.getMessage());
        }

        return false;
    }

    /**
     * Extracts the username from a validated JWT token.
     *
     * @param token The raw JWT string.
     * @return The subject (username) contained in the token.
     */
    public String getUserNameFromJwtToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        
        return claims.getSubject();
    }

    /**
     * Creates the SecretKey from the configured string.
     * JJWT requires robust keys for HMAC signing, decoding from base64.
     *
     * @return A cryptographic SecretKey instance.
     */
    private SecretKey key() {
        // If your secret is simple text, it should be long enough and you could just use getBytes().
        // It's assumed your secret is base64 encoded config property if using Decoders.BASE64
        // Example for plain text string: return Keys.hmacShaKeyFor(jwtSecret.getBytes());
        // Here we use basic string bytes for simplicity assuming 256bit secret length:
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }
}
