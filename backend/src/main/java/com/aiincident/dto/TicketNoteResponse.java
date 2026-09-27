package com.aiincident.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketNoteResponse {
    private Long id;
    private String authorName;
    private String content;
    private LocalDateTime createdAt;
}
