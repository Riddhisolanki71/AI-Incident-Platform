package com.aiincident.controller;

import com.aiincident.dto.RegisterRequest;
import com.aiincident.dto.UserSummary;
import com.aiincident.model.Role;
import com.aiincident.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * Agents/admins that tickets can be assigned to - used to populate the assign
     * dropdown on the support dashboard. Restricted to staff: this list includes
     * every agent's and admin's name and email, which customers have no
     * legitimate reason to see (previously this endpoint had no role check and
     * was reachable by any authenticated CUSTOMER).
     */
    @GetMapping("/agents")
    @PreAuthorize("hasAnyRole('SUPPORT_AGENT','ADMIN')")
    public ResponseEntity<List<UserSummary>> listAgents() {
        return ResponseEntity.ok(userService.listAgentsAndAdmins());
    }

    @PostMapping("/agents")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserSummary> createAgent(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userService.createStaffUser(request, Role.SUPPORT_AGENT));
    }
}
