package com.aiincident.service;

import com.aiincident.model.Sentiment;
import com.aiincident.model.Severity;
import com.aiincident.model.TicketCategory;
import com.aiincident.model.TicketPriority;

/**
 * Provider-agnostic result of analyzing a ticket. Both the real (Anthropic) and
 * mock implementations of AIAnalysisService return this same shape so the rest
 * of the application never needs to know which provider produced it.
 */
public record AIAnalysisResult(
        TicketCategory category,
        Severity severity,
        Sentiment sentiment,
        int emotionalIntensity,
        TicketPriority priority,
        String summary,
        String suggestedResolution,
        String analyzedBy
) {
}
