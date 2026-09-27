package com.aiincident.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Set;
import java.util.function.Function;

@Slf4j
@Component
public class JwtUtil {

    // The exact placeholder shipped in application.yml's default value. If this is
    // still in use, the deployment was never given a real secret via JWT_SECRET.
    private static final Set<String> KNOWN_INSECURE_DEFAULTS = Set.of(
            "CHANGE_ME_DEV_ONLY_CHANGE_ME_DEV_ONLY_CHANGE_ME"
    );

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtUtil(@Value("${app.jwt.secret}") String secret,
                   @Value("${app.jwt.expiration-ms}") long expirationMs,
                   @Value("${spring.profiles.active:}") String activeProfiles) {
        // HS256 requires a key of at least 256 bits (32 bytes). Previously this
        // silently zero-padded shorter secrets, which quietly produces a much
        // weaker, predictable key instead of surfacing the misconfiguration.
        // Fail fast instead so a bad JWT_SECRET is caught at startup, not in
        // production traffic.
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "app.jwt.secret (JWT_SECRET) must be at least 32 bytes for HS256. " +
                    "Generate one with: openssl rand -base64 32");
        }

        boolean looksLikeDefaultPlaceholder = KNOWN_INSECURE_DEFAULTS.contains(secret);
        boolean isProdProfile = activeProfiles != null &&
                Set.of(activeProfiles.toLowerCase().split(",")).contains("prod");

        if (looksLikeDefaultPlaceholder && isProdProfile) {
            throw new IllegalStateException(
                    "Refusing to start with the default placeholder JWT secret while the 'prod' " +
                    "profile is active. Set a real JWT_SECRET environment variable.");
        }
        if (looksLikeDefaultPlaceholder) {
            log.warn("JWT_SECRET is not set - using the built-in development placeholder. " +
                    "This is fine for local development only; set a real JWT_SECRET before deploying.");
        }

        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMs = expirationMs;
    }

    public String generateToken(String email, Long userId, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(email)
                .claim("userId", userId)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Long extractUserId(String token) {
        return extractAllClaims(token).get("userId", Long.class);
    }

    public String extractRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }

    public boolean isTokenValid(String token, String expectedEmail) {
        String email = extractEmail(token);
        return email.equals(expectedEmail) && !isExpired(token);
    }

    private boolean isExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    private <T> T extractClaim(String token, Function<Claims, T> resolver) {
        Claims claims = extractAllClaims(token);
        return resolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
