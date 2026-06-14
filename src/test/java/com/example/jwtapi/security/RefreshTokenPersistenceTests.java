package com.example.jwtapi.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;

import com.example.jwtapi.user.User;
import com.example.jwtapi.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.BadCredentialsException;

@SpringBootTest
class RefreshTokenPersistenceTests {

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    private User user;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
        user = userRepository.save(new User(
                "Usuario Token",
                "refresh-token-persistence@example.com",
                "{noop}123456"
        ));
    }

    @Test
    void createsRefreshTokenWithMetadataAndHashOnly() {
        RefreshTokenService.RefreshTokenResult result = refreshTokenService.createRefreshToken(
                user,
                new RefreshTokenMetadata("Notebook", "JUnit", "203.0.113.10")
        );

        RefreshToken persistedToken = refreshTokenRepository.findAll().getFirst();

        assertNotEquals(result.token(), persistedToken.getTokenHash());
        assertEquals(64, persistedToken.getTokenHash().length());
        assertEquals("Notebook", persistedToken.getDeviceName());
        assertEquals("JUnit", persistedToken.getUserAgent());
        assertEquals("203.0.113.10", persistedToken.getIpAddress());
        assertNotNull(persistedToken.getCreatedAt());
        assertNotNull(persistedToken.getExpiresAt());
        assertFalse(persistedToken.isRevoked());
    }

    @Test
    void findsRefreshTokenByTokenHash() {
        RefreshTokenService.RefreshTokenResult result = refreshTokenService.createRefreshToken(user);

        assertTrue(refreshTokenRepository.findByTokenHash(result.refreshToken().getTokenHash()).isPresent());
    }

    @Test
    void rejectsExpiredRefreshToken() {
        RefreshTokenService.RefreshTokenResult result = refreshTokenService.createRefreshToken(user);
        result.refreshToken().setExpiresAt(Instant.now().minusSeconds(1));
        refreshTokenRepository.save(result.refreshToken());

        assertThrows(BadCredentialsException.class, () -> refreshTokenService.rotate(result.token()));
    }

    @Test
    void revokesRefreshTokenWithRevokedAt() {
        RefreshTokenService.RefreshTokenResult result = refreshTokenService.createRefreshToken(user);

        refreshTokenService.revoke(result.token());

        RefreshToken persistedToken = refreshTokenRepository.findByTokenHash(result.refreshToken().getTokenHash())
                .orElseThrow();
        assertTrue(persistedToken.isRevoked());
        assertNotNull(persistedToken.getRevokedAt());
    }

    @Test
    void rotatesRefreshTokenAndPreservesSessionMetadata() {
        RefreshTokenService.RefreshTokenResult firstToken = refreshTokenService.createRefreshToken(
                user,
                new RefreshTokenMetadata("Celular", "JUnit Mobile", "198.51.100.20")
        );

        RefreshTokenService.RefreshTokenResult secondToken = refreshTokenService.rotate(
                firstToken.token(),
                new RefreshTokenMetadata(null, null, null)
        );

        RefreshToken previousToken = refreshTokenRepository.findByTokenHash(firstToken.refreshToken().getTokenHash())
                .orElseThrow();
        RefreshToken currentToken = refreshTokenRepository.findByTokenHash(secondToken.refreshToken().getTokenHash())
                .orElseThrow();

        assertTrue(previousToken.isRevoked());
        assertNotNull(previousToken.getLastUsedAt());
        assertNotNull(previousToken.getRevokedAt());
        assertEquals("Celular", currentToken.getDeviceName());
        assertEquals("JUnit Mobile", currentToken.getUserAgent());
        assertEquals("198.51.100.20", currentToken.getIpAddress());
    }

    @Test
    void deletesExpiredAndOldRevokedTokensBeforeCutoff() {
        Instant now = Instant.now();
        Instant cutoff = now.minus(Duration.ofDays(30));

        RefreshToken expiredToken = refreshTokenService.createRefreshToken(user).refreshToken();
        expiredToken.setExpiresAt(now.minus(Duration.ofDays(40)));
        refreshTokenRepository.save(expiredToken);

        RefreshToken revokedToken = refreshTokenService.createRefreshToken(user).refreshToken();
        revokedToken.revoke(now.minus(Duration.ofDays(40)));
        refreshTokenRepository.save(revokedToken);

        RefreshToken activeToken = refreshTokenService.createRefreshToken(user).refreshToken();

        int deleted = refreshTokenService.deleteExpiredOrRevokedBefore(cutoff);

        assertEquals(2, deleted);
        assertFalse(refreshTokenRepository.findByTokenHash(expiredToken.getTokenHash()).isPresent());
        assertFalse(refreshTokenRepository.findByTokenHash(revokedToken.getTokenHash()).isPresent());
        assertTrue(refreshTokenRepository.findByTokenHash(activeToken.getTokenHash()).isPresent());
    }
}
