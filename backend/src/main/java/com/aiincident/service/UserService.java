package com.aiincident.service;

import com.aiincident.dto.RegisterRequest;
import com.aiincident.dto.UserSummary;
import com.aiincident.exception.DuplicateResourceException;
import com.aiincident.model.Role;
import com.aiincident.model.User;
import com.aiincident.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public List<UserSummary> listAgentsAndAdmins() {
        return userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.SUPPORT_AGENT || u.getRole() == Role.ADMIN)
                .map(this::toSummary)
                .toList();
    }

    /** Admin-only: create a SUPPORT_AGENT or ADMIN account. */
    @Transactional
    public UserSummary createStaffUser(RegisterRequest request, Role role) {
        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("An account with this email already exists");
        }
        if (role == Role.CUSTOMER) {
            throw new IllegalArgumentException("Use /api/auth/register for customer accounts");
        }

        User user = User.builder()
                .name(request.getName().trim())
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .build();

        return toSummary(userRepository.save(user));
    }

    private UserSummary toSummary(User user) {
        return UserSummary.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }
}
