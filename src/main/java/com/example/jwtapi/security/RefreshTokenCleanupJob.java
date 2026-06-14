package com.example.jwtapi.security;

import java.time.Duration;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenCleanupJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(RefreshTokenCleanupJob.class);

    private final RefreshTokenService refreshTokenService;
    private final Duration retention;

    public RefreshTokenCleanupJob(
            RefreshTokenService refreshTokenService,
            @Value("${app.refresh-token.cleanup.retention}") Duration retention
    ) {
        this.refreshTokenService = refreshTokenService;
        this.retention = retention;
    }

    @Scheduled(fixedDelayString = "${app.refresh-token.cleanup.interval}")
    public void cleanExpiredAndRevokedTokens() {
        Instant cutoff = Instant.now().minus(retention);
        int deleted = refreshTokenService.deleteExpiredOrRevokedBefore(cutoff);
        LOGGER.info("Refresh token cleanup finished. deleted={}, cutoff={}", deleted, cutoff);
    }
}
