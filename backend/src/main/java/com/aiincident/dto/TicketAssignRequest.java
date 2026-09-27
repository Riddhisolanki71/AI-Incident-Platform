package com.aiincident.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TicketAssignRequest {

    @NotNull(message = "Agent id is required")
    private Long agentId;
}
