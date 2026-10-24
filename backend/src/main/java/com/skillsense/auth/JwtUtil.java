package com.skillsense.auth;

import com.skillsense.user.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expirationMs;

    public JwtUtil(
            @Value("${app.jwt.secret:skillsense_secret_key_minimum_256_bits_length_for_hs256_algorithm_123456}") String secret
    ) {
        // Enforce at least 256 bits (32 bytes)
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
            keyBytes = padded;
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
        // 8 hours in milliseconds = 8 * 60 * 60 * 1000 = 28,800,000 ms
        this.expirationMs = 8 * 60 * 60 * 1000L;
    }

    public String generateToken(Long userId, String collegeId, Role role, String name) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(collegeId)
                .claim("userId", userId)
                .claim("role", role.name())
                .claim("name", name)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    public Claims parseAndValidateToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validateToken(String token) {
        try {
            parseAndValidateToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String getCollegeIdFromToken(String token) {
        return parseAndValidateToken(token).getSubject();
    }

    public String getRoleFromToken(String token) {
        return parseAndValidateToken(token).get("role", String.class);
    }

    public Long getUserIdFromToken(String token) {
        Number id = parseAndValidateToken(token).get("userId", Number.class);
        return id != null ? id.longValue() : null;
    }
}
