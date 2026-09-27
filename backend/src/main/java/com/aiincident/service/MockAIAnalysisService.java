package com.aiincident.service;

import com.aiincident.model.Sentiment;
import com.aiincident.model.Severity;
import com.aiincident.model.TicketCategory;
import com.aiincident.model.TicketPriority;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * ============================== MOCK IMPLEMENTATION ==============================
 * THIS IS **NOT** REAL AI. It is a deterministic, rule-based placeholder used only:
 *   1) when app.ai.provider=mock (e.g. no Anthropic API key available yet), or
 *   2) as an automatic fallback if the real Anthropic API call fails/errors.
 *
 * Every record produced here is stamped with analyzedBy="mock-rule-based" so the
 * frontend and database can always show the user which analyses are real AI
 * output versus this placeholder. Do not present this service's output as AI-driven
 * anywhere in the UI or documentation.
 *
 * To replace: implement AIAnalysisService with a real provider (see
 * AnthropicAIAnalysisService) and set app.ai.provider accordingly. This class can
 * then remain purely as an offline/dev fallback.
 * ===================================================================================
 */
@Service("mockAIAnalysisService")
public class MockAIAnalysisService implements AIAnalysisService {

    private static final String ANALYZED_BY = "mock-rule-based (NOT real AI)";

    @Override
    public AIAnalysisResult analyze(String title, String description, TicketCategory customerCategory) {
        String text = ((title == null ? "" : title) + " " + (description == null ? "" : description))
                .toLowerCase(Locale.ROOT);

        TicketCategory category = guessCategory(text, customerCategory);
        Sentiment sentiment = guessSentiment(text);
        Severity severity = guessSeverity(text, sentiment);
        int emotionalIntensity = guessIntensity(text, sentiment);
        TicketPriority priority = derivePriority(severity, sentiment);

        String summary = "Customer reported an issue related to " + category.name().toLowerCase(Locale.ROOT)
                + "; the ticket text suggests " + sentiment.name().toLowerCase(Locale.ROOT) + " sentiment.";
        String suggestedResolution = defaultResolutionFor(category);

        return new AIAnalysisResult(category, severity, sentiment, emotionalIntensity, priority,
                summary, suggestedResolution, ANALYZED_BY);
    }

    private TicketCategory guessCategory(String text, TicketCategory customerCategory) {
        if (containsAny(text, "refund", "charge", "payment", "billed", "invoice", "money")) return TicketCategory.PAYMENT;
        if (containsAny(text, "login", "password", "account", "locked", "sign in")) return TicketCategory.ACCOUNT;
        // NOTE: "app" was previously in this list and was a real bug - as a naive
        // substring it matched inside unrelated words like "happy", "application",
        // "appreciate", silently miscategorizing plenty of non-technical tickets as
        // TECHNICAL. Removed; the remaining keywords are specific enough on their own.
        if (containsAny(text, "error", "bug", "crash", "not working", "broken", "application")) return TicketCategory.TECHNICAL;
        if (containsAny(text, "delivery", "shipping", "package", "courier", "shipped")) return TicketCategory.DELIVERY;
        if (containsAny(text, "product", "quality", "defective", "item")) return TicketCategory.PRODUCT;
        return customerCategory != null ? customerCategory : TicketCategory.OTHER;
    }

    private Sentiment guessSentiment(String text) {
        if (containsAny(text, "angry", "furious", "unacceptable", "terrible", "worst", "immediately", "urgent", "asap"))
            return Sentiment.NEGATIVE;
        if (containsAny(text, "thanks", "great", "love", "happy", "appreciate"))
            return Sentiment.POSITIVE;
        // NOTE: a bare "not" used to be in this list and was a real bug - as a
        // naive substring it matched inside unrelated words like "notification",
        // "note", "notify", silently flagging plenty of neutral/positive tickets as
        // NEGATIVE. Removed; "cannot"/"can't"/"failed"/"wrong" already cover the
        // same intent far more precisely.
        if (containsAny(text, "issue", "problem", "cannot", "can't", "failed", "wrong"))
            return Sentiment.NEGATIVE;
        return Sentiment.NEUTRAL;
    }

    private Severity guessSeverity(String text, Sentiment sentiment) {
        if (containsAny(text, "twice", "duplicate", "down", "cannot access", "lost", "security", "fraud")) return Severity.HIGH;
        if (sentiment == Sentiment.NEGATIVE) return Severity.MEDIUM;
        return Severity.LOW;
    }

    private int guessIntensity(String text, Sentiment sentiment) {
        int base = switch (sentiment) {
            case NEGATIVE -> 6;
            case NEUTRAL -> 3;
            case POSITIVE -> 1;
        };
        if (containsAny(text, "immediately", "urgent", "asap", "furious", "unacceptable")) base += 3;
        return Math.min(10, base);
    }

    private TicketPriority derivePriority(Severity severity, Sentiment sentiment) {
        if (severity == Severity.HIGH) return TicketPriority.HIGH;
        if (severity == Severity.MEDIUM && sentiment == Sentiment.NEGATIVE) return TicketPriority.HIGH;
        if (severity == Severity.MEDIUM) return TicketPriority.MEDIUM;
        return TicketPriority.LOW;
    }

    private String defaultResolutionFor(TicketCategory category) {
        return switch (category) {
            case PAYMENT -> "Verify the transaction history in the payment gateway and refund any confirmed duplicate or erroneous charge.";
            case ACCOUNT -> "Verify the customer's identity, then reset credentials or unlock the account as appropriate.";
            case TECHNICAL -> "Reproduce the reported error, check recent logs/deploys, and escalate to engineering if it persists.";
            case DELIVERY -> "Check the shipment tracking status with the carrier and provide the customer an updated ETA or reship if lost.";
            case PRODUCT -> "Confirm the product defect with the customer (photos if applicable) and offer a replacement or refund per policy.";
            case OTHER -> "Review the full ticket description with the customer to clarify the request before proceeding.";
        };
    }

    /**
     * Checks whether any keyword genuinely appears as a whole word (or, for
     * multi-word phrases, as that exact phrase) in the text.
     *
     * Originally this used a plain String.contains(), which matches keywords as
     * raw substrings anywhere - so "app" matched inside "happy", and "not" matched
     * inside "notification". That produced real, silent miscategorizations (see
     * the notes on guessCategory/guessSentiment above and the regression tests in
     * MockAIAnalysisServiceTest). Single-word keywords are now matched on word
     * boundaries; multi-word phrases (which already contain their own boundaries)
     * are still matched with contains().
     */
    private boolean containsAny(String text, String... keywords) {
        for (String k : keywords) {
            if (k.indexOf(' ') >= 0) {
                if (text.contains(k)) return true;
            } else if (Pattern.compile("\\b" + Pattern.quote(k) + "\\b").matcher(text).find()) {
                return true;
            }
        }
        return false;
    }
}
