package com.aiincident.controller;

import com.aiincident.dto.*;
import com.aiincident.model.TicketCategory;
import com.aiincident.model.TicketPriority;
import com.aiincident.model.TicketStatus;
import com.aiincident.security.SecurityUtils;
import com.aiincident.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    /** Customers create tickets; AI analysis runs automatically. */
    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<TicketResponse> createTicket(@Valid @RequestBody TicketCreateRequest request) {
        String email = SecurityUtils.currentUserEmail();
        return ResponseEntity.status(HttpStatus.CREATED).body(ticketService.createTicket(email, request));
    }

    /** A customer's own tickets. */
    @GetMapping("/mine")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<TicketResponse>> getMyTickets() {
        return ResponseEntity.ok(ticketService.getMyTickets(SecurityUtils.currentUserEmail()));
    }

    /** Support dashboard: list/filter/search all tickets. */
    @GetMapping
    @PreAuthorize("hasAnyRole('SUPPORT_AGENT','ADMIN')")
    public ResponseEntity<List<TicketResponse>> searchTickets(
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) TicketPriority priority,
            @RequestParam(required = false) TicketCategory category,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(ticketService.searchTickets(status, priority, category, search));
    }

    /** Ticket details. Accessible to the owning customer, or any support agent/admin. */
    @GetMapping("/{id}")
    public ResponseEntity<TicketResponse> getTicket(@PathVariable Long id) {
        return ResponseEntity.ok(ticketService.getTicketById(id, SecurityUtils.currentUserEmail()));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SUPPORT_AGENT','ADMIN')")
    public ResponseEntity<TicketResponse> updateStatus(@PathVariable Long id,
                                                        @Valid @RequestBody TicketStatusUpdateRequest request) {
        return ResponseEntity.ok(ticketService.updateStatus(id, request));
    }

    @PatchMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('SUPPORT_AGENT','ADMIN')")
    public ResponseEntity<TicketResponse> assignAgent(@PathVariable Long id,
                                                       @Valid @RequestBody TicketAssignRequest request) {
        return ResponseEntity.ok(ticketService.assignAgent(id, request));
    }

    @PostMapping("/{id}/notes")
    @PreAuthorize("hasAnyRole('SUPPORT_AGENT','ADMIN')")
    public ResponseEntity<TicketResponse> addNote(@PathVariable Long id,
                                                   @Valid @RequestBody TicketNoteRequest request) {
        return ResponseEntity.ok(ticketService.addNote(id, SecurityUtils.currentUserEmail(), request));
    }
}
