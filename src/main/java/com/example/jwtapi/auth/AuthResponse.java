package com.example.jwtapi.auth;

public record AuthResponse(
        String token,
        Long id,
        String name,
        String email
) {
}
