package com.aiincident.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Used both for public self-registration (/api/auth/register, always creates a
 * CUSTOMER) and for admin-created staff accounts (/api/users/agents, role is
 * supplied separately by the controller - see UserService.createStaffUser).
 *
 * There is deliberately no "role" field here: an earlier version had one, but
 * every caller ignored whatever value was sent in it (self-registration always
 * hardcodes CUSTOMER; staff creation always uses the role the controller
 * passes in). An accepted-but-silently-ignored field is a validation gap in
 * itself - a client could reasonably assume sending "role": "ADMIN" here does
 * something. Removing it makes the actual contract explicit.
 */
@Data
public class RegisterRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must be at most 100 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 255, message = "Email must be at most 255 characters")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    private String password;
}
