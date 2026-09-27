package com.aiincident.service;

import com.aiincident.model.Sentiment;
import com.aiincident.model.TicketCategory;
import com.aiincident.model.TicketPriority;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MockAIAnalysisServiceTest {

    private final MockAIAnalysisService service = new MockAIAnalysisService();

    @Test
    void analyze_detectsPaymentIssueAsHighPriorityAndNegativeSentiment() {
        AIAnalysisResult result = service.analyze(
                "Duplicate charge on my account",
                "Money was deducted from my account twice and my order is still showing pending. Please fix this immediately.",
                TicketCategory.PAYMENT
        );

        assertThat(result.category()).isEqualTo(TicketCategory.PAYMENT);
        assertThat(result.sentiment()).isEqualTo(Sentiment.NEGATIVE);
        assertThat(result.priority()).isEqualTo(TicketPriority.HIGH);
        assertThat(result.analyzedBy()).contains("mock");
        assertThat(result.summary()).isNotBlank();
        assertThat(result.suggestedResolution()).isNotBlank();
        assertThat(result.emotionalIntensity()).isBetween(1, 10);
    }

    @Test
    void analyze_detectsNeutralLowPriorityForCalmText() {
        AIAnalysisResult result = service.analyze(
                "Question about product",
                "I would like to know more details about this product before I decide to buy it.",
                TicketCategory.PRODUCT
        );

        assertThat(result.priority()).isIn(TicketPriority.LOW, TicketPriority.MEDIUM);
        assertThat(result.analyzedBy()).contains("NOT real AI");
    }

    @Test
    void analyze_neverReturnsNullFields() {
        AIAnalysisResult result = service.analyze("", "", TicketCategory.OTHER);

        assertThat(result.category()).isNotNull();
        assertThat(result.severity()).isNotNull();
        assertThat(result.sentiment()).isNotNull();
        assertThat(result.priority()).isNotNull();
    }

    // --- Regression tests for the substring-matching bug found during audit ---
    // containsAny() used to call String.contains() directly, so single-word
    // keywords matched anywhere inside a longer word, not just as whole words.

    @Test
    void analyze_wordHappyDoesNotFalsePositiveAsTechnicalCategory() {
        // "happy" contains "app" as a raw substring. The old implementation had
        // "app" as a TECHNICAL keyword, so any grateful/positive ticket mentioning
        // being "happy" was silently miscategorized as TECHNICAL.
        AIAnalysisResult result = service.analyze(
                "Thank you",
                "I am so happy with how quickly my question was answered, thanks a lot!",
                TicketCategory.OTHER
        );

        assertThat(result.category()).isNotEqualTo(TicketCategory.TECHNICAL);
        assertThat(result.sentiment()).isEqualTo(Sentiment.POSITIVE);
    }

    @Test
    void analyze_wordNotificationDoesNotFalsePositiveAsNegativeSentiment() {
        // "notification" contains "not" as a raw substring. The old implementation
        // had a bare "not" as a NEGATIVE sentiment keyword, so a perfectly neutral
        // ticket about notification settings was silently flagged NEGATIVE.
        AIAnalysisResult result = service.analyze(
                "Notification settings",
                "I would like to know how to change my email notification preferences.",
                TicketCategory.OTHER
        );

        assertThat(result.sentiment()).isNotEqualTo(Sentiment.NEGATIVE);
    }

    @Test
    void analyze_genuineTechnicalKeywordStillMatches() {
        // Guards against over-correcting the word-boundary fix into matching nothing.
        AIAnalysisResult result = service.analyze(
                "App keeps crashing",
                "The application crashes every time I open the settings screen. This is a clear bug.",
                TicketCategory.OTHER
        );

        assertThat(result.category()).isEqualTo(TicketCategory.TECHNICAL);
    }
}
