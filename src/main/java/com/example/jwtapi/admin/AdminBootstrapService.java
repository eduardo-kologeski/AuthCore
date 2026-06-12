package com.example.jwtapi.admin;

import com.example.jwtapi.user.Role;
import com.example.jwtapi.user.User;
import com.example.jwtapi.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminBootstrapService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String bootstrapToken;

    public AdminBootstrapService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.bootstrap-token}") String bootstrapToken
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.bootstrapToken = bootstrapToken;
    }

    @Transactional
    public AdminUserResponse bootstrap(BootstrapAdminRequest request) {
        if (!bootstrapToken.equals(request.bootstrapToken())) {
            throw new BadCredentialsException("Bootstrap token invalido.");
        }

        if (userRepository.existsByRole(Role.ADMIN)) {
            throw new IllegalArgumentException("Administrador inicial ja foi criado.");
        }

        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("E-mail ja cadastrado.");
        }

        User admin = userRepository.save(new User(
                request.name().trim(),
                email,
                passwordEncoder.encode(request.password()),
                Role.ADMIN
        ));

        return AdminUserResponse.from(admin);
    }
}
