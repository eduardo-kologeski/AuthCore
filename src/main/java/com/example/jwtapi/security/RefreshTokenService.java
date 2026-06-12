package com.example.jwtapi.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

import com.example.jwtapi.user.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenService {

    private static final int TOKEN_BYTES = 64;

    private final RefreshTokenRepository refreshTokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Duration expiration;

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            @Value("${app.refresh-token.expiration}") Duration expiration
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.expiration = expiration;
    }

    @Transactional
    public RefreshTokenResult createRefreshToken(User user) {
        String token = generateToken();
        Instant now = Instant.now();
        RefreshToken refreshToken = new RefreshToken(user, hash(token), now, now.plus(expiration));
        refreshTokenRepository.save(refreshToken);
        return new RefreshTokenResult(token, refreshToken);
    }

    @Transactional
    public RefreshTokenResult rotate(String rawToken) {
        RefreshToken currentToken = findValidToken(rawToken);
        currentToken.revoke();
        return createRefreshToken(currentToken.getUser());
    }

    @Transactional
    public void revoke(String rawToken) {
        RefreshToken refreshToken = findValidToken(rawToken);
        refreshToken.revoke();
    }

    private RefreshToken findValidToken(String rawToken) {
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new BadCredentialsException("Refresh token invalido."));

        if (refreshToken.isRevoked() || refreshToken.getExpiresAt().isBefore(Instant.now())) {
            throw new BadCredentialsException("Refresh token invalido.");
        }

        return refreshToken;
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 nao esta disponivel.", ex);
        }
    }

    public record RefreshTokenResult(String token, RefreshToken refreshToken) {
    }
}
