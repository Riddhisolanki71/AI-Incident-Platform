package com.aiincident.repository;

import com.aiincident.model.Ticket;
import com.aiincident.model.TicketCategory;
import com.aiincident.model.TicketPriority;
import com.aiincident.model.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long>, JpaSpecificationExecutor<Ticket> {

    List<Ticket> findByCustomerId(Long customerId);

    long countByStatus(TicketStatus status);

    long countByPriority(TicketPriority priority);

    long countByCategory(TicketCategory category);
}
