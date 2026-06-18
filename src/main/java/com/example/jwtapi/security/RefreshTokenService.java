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
        return createRefreshToken(user, new RefreshTokenMetadata(null, null, null));
    }

    @Transactional
    public RefreshTokenResult createRefreshToken(User user, RefreshTokenMetadata metadata) {
        String token = generateToken();
        Instant now = Instant.now();
        RefreshToken refreshToken = new RefreshToken(
                user,
                hash(token),
                truncate(metadata.deviceName(), 120),
                truncate(metadata.userAgent(), 512),
                truncate(metadata.ipAddress(), 45),
                now,
                now.plus(expiration)
        );
        refreshTokenRepository.save(refreshToken);
        return new RefreshTokenResult(token, refreshToken);
    }

    @Transactional
    public RefreshTokenResult rotate(String rawToken) {
        return rotate(rawToken, new RefreshTokenMetadata(null, null, null));
    }

    @Transactional
    public RefreshTokenResult rotate(String rawToken, RefreshTokenMetadata metadata) {
        RefreshToken currentToken = findValidToken(rawToken);
        Instant now = Instant.now();
        currentToken.markUsed(now);
        currentToken.revoke(now);

        RefreshTokenMetadata nextMetadata = new RefreshTokenMetadata(
                firstNonBlank(metadata.deviceName(), currentToken.getDeviceName()),
                firstNonBlank(metadata.userAgent(), currentToken.getUserAgent()),
                firstNonBlank(metadata.ipAddress(), currentToken.getIpAddress())
        );
        return createRefreshToken(currentToken.getUser(), nextMetadata);
    }

    @Transactional
    public void revoke(String rawToken) {
        RefreshToken refreshToken = findValidToken(rawToken);
        refreshToken.revoke();
    }

    @Transactional
    public int deleteExpiredOrRevokedBefore(Instant cutoff) {
        return refreshTokenRepository.deleteExpiredOrRevokedBefore(cutoff);
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

    private String firstNonBlank(String first, String fallback) {
        return first == null || first.isBlank() ? fallback : first;
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
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
