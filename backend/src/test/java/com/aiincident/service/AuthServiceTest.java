package com.aiincident.service;

import com.aiincident.dto.AuthResponse;
import com.aiincident.dto.LoginRequest;
import com.aiincident.dto.RegisterRequest;
import com.aiincident.exception.DuplicateResourceException;
import com.aiincident.exception.InvalidCredentialsException;
import com.aiincident.model.Role;
import com.aiincident.model.User;
import com.aiincident.repository.UserRepository;
import com.aiincident.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setName("Jane Doe");
        registerRequest.setEmail("jane@example.com");
        registerRequest.setPassword("supersecret1");
    }

    @Test
    void register_validRequest_createsCustomerAndReturnsToken() {
        when(userRepository.existsByEmail("jane@example.com")).thenReturn(false);
        when(passwordEncoder.encode("supersecret1")).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(jwtUtil.generateToken(eq("jane@example.com"), eq(1L), eq("CUSTOMER"))).thenReturn("fake-jwt");

        AuthResponse response = authService.register(registerRequest);

        assertThat(response.getToken()).isEqualTo("fake-jwt");
        assertThat(response.getEmail()).isEqualTo("jane@example.com");
        assertThat(response.getRole()).isEqualTo(Role.CUSTOMER);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_duplicateEmail_throwsDuplicateResourceException() {
        when(userRepository.existsByEmail("jane@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(DuplicateResourceException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void login_validCredentials_returnsToken() {
        User existing = User.builder()
                .id(2L)
                .name("Jane Doe")
                .email("jane@example.com")
                .password("hashed-password")
                .role(Role.CUSTOMER)
                .build();

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("jane@example.com");
        loginRequest.setPassword("supersecret1");

        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(existing));
        when(passwordEncoder.matches("supersecret1", "hashed-password")).thenReturn(true);
        when(jwtUtil.generateToken("jane@example.com", 2L, "CUSTOMER")).thenReturn("fake-jwt");

        AuthResponse response = authService.login(loginRequest);

        assertThat(response.getToken()).isEqualTo("fake-jwt");
    }

    @Test
    void login_wrongPassword_throwsInvalidCredentials() {
        User existing = User.builder()
                .id(2L).name("Jane").email("jane@example.com")
                .password("hashed-password").role(Role.CUSTOMER).build();

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("jane@example.com");
        loginRequest.setPassword("wrong-password");

        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(existing));
        when(passwordEncoder.matches("wrong-password", "hashed-password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_unknownEmail_throwsInvalidCredentials() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("nobody@example.com");
        loginRequest.setPassword("whatever1");

        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}
