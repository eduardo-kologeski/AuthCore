package com.example.jwtapi.auth;

import com.example.jwtapi.security.JwtService;
import com.example.jwtapi.security.RefreshTokenMetadata;
import com.example.jwtapi.security.RefreshTokenService;
import com.example.jwtapi.user.User;
import com.example.jwtapi.user.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenService refreshTokenService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("E-mail ja cadastrado.");
        }

        User user = userRepository.save(new User(
                request.name().trim(),
                email,
                passwordEncoder.encode(request.password())
        ));

        return accessTokenResponse(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request, RefreshTokenMetadata metadata) {
        String email = request.email().trim().toLowerCase();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Credenciais invalidas."));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BadCredentialsException("Credenciais invalidas.");
        }

        if (!request.rememberMe()) {
            return accessTokenResponse(user);
        }

        RefreshTokenService.RefreshTokenResult refreshToken = refreshTokenService.createRefreshToken(
                user,
                new RefreshTokenMetadata(request.deviceName(), metadata.userAgent(), metadata.ipAddress())
        );
        return refreshTokenResponse(user, refreshToken.token());
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request, RefreshTokenMetadata metadata) {
        RefreshTokenService.RefreshTokenResult refreshToken = refreshTokenService.rotate(request.refreshToken(), metadata);
        return refreshTokenResponse(refreshToken.refreshToken().getUser(), refreshToken.token());
    }

    @Transactional
    public void logout(LogoutRequest request) {
        refreshTokenService.revoke(request.refreshToken());
    }

    private AuthResponse accessTokenResponse(User user) {
        return new AuthResponse(
                jwtService.generateToken(user),
                null,
                null
        );
    }

    private AuthResponse refreshTokenResponse(User user, String refreshToken) {
        return new AuthResponse(
                jwtService.generateToken(user),
                refreshToken,
                jwtService.getExpirationSeconds()
        );
    }
}
