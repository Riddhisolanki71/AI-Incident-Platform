package com.aiincident.config;

import com.aiincident.model.Role;
import com.aiincident.model.User;
import com.aiincident.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Creates a single default ADMIN account on first startup so there's a way into
 * the system (self-registration only ever creates CUSTOMER accounts). Configure
 * via ADMIN_EMAIL / ADMIN_PASSWORD env vars. If they're not set, no admin is
 * seeded and one must be created directly in the database or via a follow-up
 * migration - this keeps the base version from ever seeding a guessable default
 * password.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${ADMIN_EMAIL:}")
    private String adminEmail;

    @Value("${ADMIN_PASSWORD:}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        if (adminEmail == null || adminEmail.isBlank() || adminPassword == null || adminPassword.isBlank()) {
            log.info("ADMIN_EMAIL/ADMIN_PASSWORD not set - skipping admin seed. " +
                    "Set both env vars and restart to bootstrap an admin account.");
            return;
        }

        String normalizedEmail = adminEmail.trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            return;
        }

        User admin = User.builder()
                .name("Admin")
                .email(normalizedEmail)
                .password(passwordEncoder.encode(adminPassword))
                .role(Role.ADMIN)
                .build();

        userRepository.save(admin);
        log.info("Seeded default ADMIN account for {}", normalizedEmail);
    }
}
