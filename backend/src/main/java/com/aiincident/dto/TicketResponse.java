package com.aiincident.dto;

import com.aiincident.model.TicketCategory;
import com.aiincident.model.TicketPriority;
import com.aiincident.model.TicketStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketResponse {
    private Long id;
    private String title;
    private String description;
    private TicketCategory category;
    private TicketStatus status;
    private TicketPriority priority;

    private UserSummary customer;
    private UserSummary assignedAgent;

    private TicketAnalysisResponse analysis;
    private List<TicketNoteResponse> notes;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
