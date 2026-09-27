package com.aiincident.service;

import com.aiincident.dto.*;
import com.aiincident.exception.ForbiddenOperationException;
import com.aiincident.exception.ResourceNotFoundException;
import com.aiincident.model.*;
import com.aiincident.repository.TicketNoteRepository;
import com.aiincident.repository.TicketRepository;
import com.aiincident.repository.TicketSpecifications;
import com.aiincident.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final TicketNoteRepository ticketNoteRepository;
    private final AIAnalysisService aiAnalysisService;

    @Transactional
    public TicketResponse createTicket(String customerEmail, TicketCreateRequest request) {
        User customer = getUserOrThrow(customerEmail);

        Ticket ticket = Ticket.builder()
                .title(request.getTitle().trim())
                .description(request.getDescription().trim())
                .category(request.getCategory())
                .status(TicketStatus.OPEN)
                .priority(TicketPriority.MEDIUM) // placeholder until AI analysis sets the real value below
                .customer(customer)
                .build();

        Ticket saved = ticketRepository.save(ticket);

        // Run AI analysis right away and attach it to the ticket. If analysis fails
        // for any reason, the ticket is still created (see AIAnalysisService impls,
        // which fall back to the mock rather than throwing).
        AIAnalysisResult result = aiAnalysisService.analyze(saved.getTitle(), saved.getDescription(), saved.getCategory());

        TicketAnalysis analysis = TicketAnalysis.builder()
                .ticket(saved)
                .aiCategory(result.category())
                .severity(result.severity())
                .sentiment(result.sentiment())
                .emotionalIntensity(result.emotionalIntensity())
                .aiPriority(result.priority())
                .summary(result.summary())
                .suggestedResolution(result.suggestedResolution())
                .analyzedBy(result.analyzedBy())
                .build();

        saved.setAnalysis(analysis);
        saved.setPriority(result.priority()); // let the AI-determined priority drive the actual ticket priority
        Ticket withAnalysis = ticketRepository.save(saved);

        return toResponse(withAnalysis);
    }

    public TicketResponse getTicketById(Long ticketId, String requesterEmail) {
        Ticket ticket = getTicketOrThrow(ticketId);
        User requester = getUserOrThrow(requesterEmail);
        assertCanView(ticket, requester);
        return toResponse(ticket);
    }

    public List<TicketResponse> getMyTickets(String customerEmail) {
        User customer = getUserOrThrow(customerEmail);
        return ticketRepository.findByCustomerId(customer.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    /** Support-agent/admin ticket list with optional filters and free-text search. */
    public List<TicketResponse> searchTickets(TicketStatus status, TicketPriority priority,
                                               TicketCategory category, String search) {
        var spec = TicketSpecifications.filter(status, priority, category, search);
        return ticketRepository.findAll(spec).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public TicketResponse updateStatus(Long ticketId, TicketStatusUpdateRequest request) {
        Ticket ticket = getTicketOrThrow(ticketId);
        ticket.setStatus(request.getStatus());
        return toResponse(ticketRepository.save(ticket));
    }

    @Transactional
    public TicketResponse assignAgent(Long ticketId, TicketAssignRequest request) {
        Ticket ticket = getTicketOrThrow(ticketId);
        User agent = userRepository.findById(request.getAgentId())
                .orElseThrow(() -> new ResourceNotFoundException("Agent not found: " + request.getAgentId()));

        if (agent.getRole() != Role.SUPPORT_AGENT && agent.getRole() != Role.ADMIN) {
            throw new IllegalArgumentException("Selected user is not a support agent or admin");
        }

        ticket.setAssignedAgent(agent);
        if (ticket.getStatus() == TicketStatus.OPEN) {
            ticket.setStatus(TicketStatus.IN_PROGRESS);
        }
        return toResponse(ticketRepository.save(ticket));
    }

    @Transactional
    public TicketResponse addNote(Long ticketId, String authorEmail, TicketNoteRequest request) {
        Ticket ticket = getTicketOrThrow(ticketId);
        User author = getUserOrThrow(authorEmail);

        if (author.getRole() == Role.CUSTOMER) {
            throw new ForbiddenOperationException("Customers cannot add internal notes");
        }

        TicketNote note = TicketNote.builder()
                .ticket(ticket)
                .author(author)
                .content(request.getContent().trim())
                .build();
        ticketNoteRepository.save(note);

        // toResponse() re-reads notes via ticketNoteRepository directly, so it
        // already reflects the note just saved - re-fetching the ticket itself
        // here was a redundant extra DB round trip for no benefit.
        return toResponse(ticket);
    }

    // ---------------------------------------------------------------------

    private void assertCanView(Ticket ticket, User requester) {
        boolean isOwner = ticket.getCustomer().getId().equals(requester.getId());
        boolean isStaff = requester.getRole() == Role.SUPPORT_AGENT || requester.getRole() == Role.ADMIN;
        if (!isOwner && !isStaff) {
            throw new ForbiddenOperationException("You do not have access to this ticket");
        }
    }

    private Ticket getTicketOrThrow(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + id));
    }

    private User getUserOrThrow(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }

    private TicketResponse toResponse(Ticket ticket) {
        TicketAnalysisResponse analysisResponse = null;
        if (ticket.getAnalysis() != null) {
            TicketAnalysis a = ticket.getAnalysis();
            analysisResponse = TicketAnalysisResponse.builder()
                    .aiCategory(a.getAiCategory())
                    .severity(a.getSeverity())
                    .sentiment(a.getSentiment())
                    .emotionalIntensity(a.getEmotionalIntensity())
                    .aiPriority(a.getAiPriority())
                    .summary(a.getSummary())
                    .suggestedResolution(a.getSuggestedResolution())
                    .analyzedBy(a.getAnalyzedBy())
                    .build();
        }

        List<TicketNoteResponse> notes = ticketNoteRepository.findByTicketIdOrderByCreatedAtAsc(ticket.getId()).stream()
                .map(n -> TicketNoteResponse.builder()
                        .id(n.getId())
                        .authorName(n.getAuthor().getName())
                        .content(n.getContent())
                        .createdAt(n.getCreatedAt())
                        .build())
                .toList();

        return TicketResponse.builder()
                .id(ticket.getId())
                .title(ticket.getTitle())
                .description(ticket.getDescription())
                .category(ticket.getCategory())
                .status(ticket.getStatus())
                .priority(ticket.getPriority())
                .customer(toSummary(ticket.getCustomer()))
                .assignedAgent(ticket.getAssignedAgent() != null ? toSummary(ticket.getAssignedAgent()) : null)
                .analysis(analysisResponse)
                .notes(notes)
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .build();
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
