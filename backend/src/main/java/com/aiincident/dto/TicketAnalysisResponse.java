package com.aiincident.dto;

import com.aiincident.model.Sentiment;
import com.aiincident.model.Severity;
import com.aiincident.model.TicketCategory;
import com.aiincident.model.TicketPriority;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketAnalysisResponse {
    private TicketCategory aiCategory;
    private Severity severity;
    private Sentiment sentiment;
    private Integer emotionalIntensity;
    private TicketPriority aiPriority;
    private String summary;
    private String suggestedResolution;
    private String analyzedBy;
}
