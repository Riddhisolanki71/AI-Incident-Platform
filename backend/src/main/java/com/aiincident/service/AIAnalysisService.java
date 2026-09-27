package com.aiincident.service;

import com.aiincident.model.TicketCategory;

/**
 * Abstraction over "analyze a support ticket with AI".
 *
 * This is the single seam between the rest of the application and whichever
 * AI provider is actually used. Swapping providers (e.g. Anthropic -> OpenAI,
 * or real -> mock for local dev) means writing a new implementation of this
 * interface and pointing app.ai.provider at it - nothing else in the codebase
 * needs to change.
 */
public interface AIAnalysisService {

    /**
     * @param title            ticket title
     * @param description      ticket description (the main text to analyze)
     * @param customerCategory the category the customer picked when creating the ticket
     * @return a structured analysis result
     */
    AIAnalysisResult analyze(String title, String description, TicketCategory customerCategory);
}
