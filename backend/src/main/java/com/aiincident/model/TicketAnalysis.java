package com.aiincident.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Stores the result of AI analysis for a single ticket.
 * Populated by AIAnalysisService right after ticket creation.
 */
@Entity
@Table(name = "ticket_analysis")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // See Ticket.analysis for why this is excluded from equals/hashCode/toString -
    // the two sides reference each other, so leaving Lombok's defaults in place
    // would recurse infinitely the first time either side is printed or logged.
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false, unique = true)
    private Ticket ticket;

    @Enumerated(EnumType.STRING)
    private TicketCategory aiCategory;

    @Enumerated(EnumType.STRING)
    private Severity severity;

    @Enumerated(EnumType.STRING)
    private Sentiment sentiment;

    /** 1 (calm) - 10 (extremely distressed) */
    private Integer emotionalIntensity;

    @Enumerated(EnumType.STRING)
    private TicketPriority aiPriority;

    @Column(length = 1000)
    private String summary;

    @Column(length = 2000)
    private String suggestedResolution;

    /** Which AI provider produced this analysis, e.g. "anthropic" or "mock". Useful for transparency. */
    private String analyzedBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
