package com.aiincident.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TicketNoteRequest {

    // Matches TicketNote.content's DB column length (2000). Without this, an
    // over-long note would fail at the database layer instead of with a clean
    // 400 - the generic exception handler would turn that DB error into an
    // opaque 500 rather than a helpful validation message.
    @NotBlank(message = "Note content is required")
    @Size(max = 2000, message = "Note must be at most 2000 characters")
    private String content;
}
