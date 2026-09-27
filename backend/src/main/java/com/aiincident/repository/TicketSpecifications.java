package com.aiincident.repository;

import com.aiincident.model.Ticket;
import com.aiincident.model.TicketCategory;
import com.aiincident.model.TicketPriority;
import com.aiincident.model.TicketStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds a dynamic JPA Specification for filtering/searching tickets on the
 * support dashboard (status, priority, category, free-text search).
 */
public final class TicketSpecifications {

    private TicketSpecifications() {
    }

    public static Specification<Ticket> filter(TicketStatus status,
                                                 TicketPriority priority,
                                                 TicketCategory category,
                                                 String search) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (priority != null) {
                predicates.add(cb.equal(root.get("priority"), priority));
            }
            if (category != null) {
                predicates.add(cb.equal(root.get("category"), category));
            }
            if (search != null && !search.isBlank()) {
                String like = "%" + search.toLowerCase() + "%";
                Predicate titleMatch = cb.like(cb.lower(root.get("title")), like);
                Predicate descMatch = cb.like(cb.lower(root.get("description")), like);
                predicates.add(cb.or(titleMatch, descMatch));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
