package com.aiincident.service;

import com.aiincident.dto.DashboardStatsResponse;
import com.aiincident.model.TicketCategory;
import com.aiincident.model.TicketPriority;
import com.aiincident.model.TicketStatus;
import com.aiincident.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final TicketRepository ticketRepository;

    public DashboardStatsResponse getStats() {
        Map<String, Long> byCategory = new LinkedHashMap<>();
        for (TicketCategory category : TicketCategory.values()) {
            byCategory.put(category.name(), ticketRepository.countByCategory(category));
        }

        return DashboardStatsResponse.builder()
                .totalTickets(ticketRepository.count())
                .openTickets(ticketRepository.countByStatus(TicketStatus.OPEN))
                .inProgressTickets(ticketRepository.countByStatus(TicketStatus.IN_PROGRESS))
                .resolvedTickets(ticketRepository.countByStatus(TicketStatus.RESOLVED))
                .closedTickets(ticketRepository.countByStatus(TicketStatus.CLOSED))
                .highPriorityTickets(ticketRepository.countByPriority(TicketPriority.HIGH))
                .ticketsByCategory(byCategory)
                .build();
    }
}
