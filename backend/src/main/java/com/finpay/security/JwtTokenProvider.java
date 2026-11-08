package com.finpay.security;

import com.finpay.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.UUID;

@Component
@Slf4j
public class JwtTokenProvider {

    /** HS512 requires a key of at least 512 bits. */
    static final int MIN_SECRET_BYTES = 64;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    /**
     * Fails fast at startup: a missing or weak signing key must stop the application instead
     * of silently running with a guessable secret (or failing later on the first login).
     */
    @PostConstruct
    void validateConfiguration() {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException(
                    "jwt.secret (JWT_SECRET) is not configured. Generate one with: openssl rand -hex 32");
        }
        if (jwtSecret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "jwt.secret (JWT_SECRET) must have at least " + MIN_SECRET_BYTES
                            + " characters for HS512. Generate one with: openssl rand -hex 32");
        }
    }

    private Key getKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Generates a token for a generic Spring Security principal. Only the subject
     * (username) claim is available here - prefer {@link #generateToken(User)} when
     * the concrete user is on hand so the token also carries email/fullName/role,
     * which the frontend relies on when decoding the token after a page reload.
     */
    public String generateToken(UserDetails userDetails) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);

        return Jwts.builder()
                .setId(UUID.randomUUID().toString())
                .setSubject(userDetails.getUsername())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * Generates a token that also carries the email, fullName and role claims.
     * The frontend (AuthContext) decodes these directly from the JWT on page
     * reload, so without them the user's name/role/email would be undefined
     * until the next login.
     * <p>
     * Every token has a unique id ({@code jti}) so it can be revoked on logout.
     */
    public String generateToken(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);

        return Jwts.builder()
                .setId(UUID.randomUUID().toString())
                .setSubject(user.getUsername())
                .claim("email", user.getEmail())
                .claim("fullName", user.getFullName())
                .claim("role", user.getRole().name())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    /** Parses and verifies signature + expiry. Throws {@link JwtException} when invalid. */
    public Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String getUsernameFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
        }
        return false;
    }
}
