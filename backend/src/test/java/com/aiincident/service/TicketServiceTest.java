package com.aiincident.service;

import com.aiincident.dto.TicketCreateRequest;
import com.aiincident.dto.TicketResponse;
import com.aiincident.dto.TicketStatusUpdateRequest;
import com.aiincident.exception.ForbiddenOperationException;
import com.aiincident.exception.ResourceNotFoundException;
import com.aiincident.model.*;
import com.aiincident.repository.TicketNoteRepository;
import com.aiincident.repository.TicketRepository;
import com.aiincident.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TicketNoteRepository ticketNoteRepository;
    @Mock
    private AIAnalysisService aiAnalysisService;

    @InjectMocks
    private TicketService ticketService;

    private User customer;

    @BeforeEach
    void setUp() {
        customer = User.builder().id(1L).name("Jane").email("jane@example.com").role(Role.CUSTOMER).build();
    }

    @Test
    void createTicket_validRequest_savesTicketWithAiAnalysis() {
        TicketCreateRequest request = new TicketCreateRequest();
        request.setTitle("Payment issue");
        request.setDescription("Money was deducted twice and the order is still pending.");
        request.setCategory(TicketCategory.PAYMENT);

        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(customer));

        Ticket saved = Ticket.builder()
                .id(10L).title(request.getTitle()).description(request.getDescription())
                .category(TicketCategory.PAYMENT).status(TicketStatus.OPEN)
                .priority(TicketPriority.MEDIUM).customer(customer).build();

        when(ticketRepository.save(any(Ticket.class))).thenReturn(saved).thenReturn(saved);

        AIAnalysisResult aiResult = new AIAnalysisResult(
                TicketCategory.PAYMENT, Severity.HIGH, Sentiment.NEGATIVE, 8,
                TicketPriority.HIGH, "Customer reports duplicate payment.",
                "Verify and refund duplicate charge.", "mock-rule-based (NOT real AI)");

        when(aiAnalysisService.analyze(any(), any(), any())).thenReturn(aiResult);
        when(ticketNoteRepository.findByTicketIdOrderByCreatedAtAsc(10L)).thenReturn(List.of());

        TicketResponse response = ticketService.createTicket("jane@example.com", request);

        assertThat(response.getAnalysis()).isNotNull();
        assertThat(response.getAnalysis().getSeverity()).isEqualTo(Severity.HIGH);
        assertThat(response.getPriority()).isEqualTo(TicketPriority.HIGH);
        verify(ticketRepository, times(2)).save(any(Ticket.class));
    }

    @Test
    void getTicketById_nonOwnerCustomer_throwsForbidden() {
        User otherCustomer = User.builder().id(2L).email("bob@example.com").role(Role.CUSTOMER).build();
        Ticket ticket = Ticket.builder().id(5L).customer(customer).status(TicketStatus.OPEN)
                .priority(TicketPriority.LOW).category(TicketCategory.OTHER).build();

        when(ticketRepository.findById(5L)).thenReturn(Optional.of(ticket));
        when(userRepository.findByEmail("bob@example.com")).thenReturn(Optional.of(otherCustomer));

        assertThatThrownBy(() -> ticketService.getTicketById(5L, "bob@example.com"))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void getTicketById_owner_returnsTicket() {
        Ticket ticket = Ticket.builder().id(5L).customer(customer).status(TicketStatus.OPEN)
                .priority(TicketPriority.LOW).category(TicketCategory.OTHER).build();

        when(ticketRepository.findById(5L)).thenReturn(Optional.of(ticket));
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(customer));
        when(ticketNoteRepository.findByTicketIdOrderByCreatedAtAsc(5L)).thenReturn(List.of());

        TicketResponse response = ticketService.getTicketById(5L, "jane@example.com");

        assertThat(response.getId()).isEqualTo(5L);
    }

    @Test
    void getTicketById_notFound_throwsResourceNotFound() {
        when(ticketRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.getTicketById(99L, "jane@example.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateStatus_validTransition_updatesTicket() {
        Ticket ticket = Ticket.builder().id(7L).customer(customer).status(TicketStatus.OPEN)
                .priority(TicketPriority.LOW).category(TicketCategory.OTHER).build();

        TicketStatusUpdateRequest request = new TicketStatusUpdateRequest();
        request.setStatus(TicketStatus.RESOLVED);

        when(ticketRepository.findById(7L)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any(Ticket.class))).thenReturn(ticket);
        when(ticketNoteRepository.findByTicketIdOrderByCreatedAtAsc(7L)).thenReturn(List.of());

        TicketResponse response = ticketService.updateStatus(7L, request);

        assertThat(response.getStatus()).isEqualTo(TicketStatus.RESOLVED);
    }
}
