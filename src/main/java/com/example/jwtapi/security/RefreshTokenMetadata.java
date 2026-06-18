package com.example.jwtapi.security;

public record RefreshTokenMetadata(
        String deviceName,
        String userAgent,
        String ipAddress
) {
}
