package com.example.jwtapi.security;

import java.util.Optional;
import java.time.Instant;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("""
            delete from RefreshToken refreshToken
            where refreshToken.expiresAt < :cutoff
               or (
                    refreshToken.revoked = true
                    and refreshToken.revokedAt is not null
                    and refreshToken.revokedAt < :cutoff
               )
            """)
    int deleteExpiredOrRevokedBefore(Instant cutoff);
}
