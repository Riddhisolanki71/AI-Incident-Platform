package com.aiincident.repository;

import com.aiincident.model.TicketAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TicketAnalysisRepository extends JpaRepository<TicketAnalysis, Long> {
    Optional<TicketAnalysis> findByTicketId(Long ticketId);
}
