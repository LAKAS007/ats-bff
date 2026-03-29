package com.diploma.atsbff.security;

import com.diploma.atsbff.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Date;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final Key signingKey;
    private final long expirationSeconds;

    public JwtService(AppProperties properties) {
        this.signingKey = Keys.hmacShaKeyFor(
            properties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8)
        );
        this.expirationSeconds = properties.getJwt().getExpirationSeconds();
    }

    public String generateToken(String username, String displayName, String role) {
        Instant now = Instant.now();
        return Jwts.builder()
            .subject(username)
            .claim("displayName", displayName)
            .claim("role", role)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(expirationSeconds)))
            .signWith(signingKey)
            .compact();
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean isTokenValid(String token, String username) {
        try {
            Claims claims = parseClaims(token);
            return username.equals(claims.getSubject())
                && claims.getExpiration().after(new Date());
        } catch (JwtException | IllegalArgumentException exception) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
            .verifyWith((javax.crypto.SecretKey) signingKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}
