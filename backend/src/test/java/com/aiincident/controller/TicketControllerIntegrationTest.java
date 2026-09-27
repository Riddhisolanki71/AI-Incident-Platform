package com.aiincident.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end tests through the real Spring context (H2 in-memory DB, mock AI
 * provider - see src/test/resources/application.yml) covering the full
 * register -> login -> create ticket -> retrieve -> update status flow, plus
 * unauthorized access and invalid-data cases.
 */
@SpringBootTest
@AutoConfigureMockMvc
class TicketControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String registerAndLogin(String email) throws Exception {
        var registerBody = Map.of("name", "Test Customer", "email", email, "password", "strongPassword1");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerBody)))
                .andExpect(status().isCreated());

        var loginBody = Map.of("email", email, "password", "strongPassword1");
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginBody)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        return json.get("token").asText();
    }

    @Test
    void createTicket_withoutToken_returns401Or403() throws Exception {
        var body = Map.of("title", "Login issue", "description", "I cannot access my account at all.",
                "category", "ACCOUNT");

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status != 401 && status != 403) {
                        throw new AssertionError("Expected 401 or 403 but got " + status);
                    }
                });
    }

    @Test
    void createTicket_validPayload_returns201WithAiAnalysis() throws Exception {
        String token = registerAndLogin("customer" + System.nanoTime() + "@example.com");

        var body = Map.of(
                "title", "Duplicate payment",
                "description", "Money was deducted from my account twice and my order is still pending. Please fix this immediately.",
                "category", "PAYMENT"
        );

        mockMvc.perform(post("/api/tickets")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.analysis").isNotEmpty())
                .andExpect(jsonPath("$.analysis.analyzedBy").exists())
                .andExpect(jsonPath("$.analysis.priority").doesNotExist()) // field name is aiPriority, sanity check shape
                .andExpect(jsonPath("$.analysis.aiPriority").exists());
    }

    @Test
    void createTicket_invalidPayload_tooShortDescription_returns400() throws Exception {
        String token = registerAndLogin("customer" + System.nanoTime() + "@example.com");

        var body = Map.of("title", "Hi", "description", "short", "category", "OTHER");

        mockMvc.perform(post("/api/tickets")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createTicket_missingCategory_returns400() throws Exception {
        String token = registerAndLogin("customer" + System.nanoTime() + "@example.com");

        var body = Map.of("title", "Hi", "description", "This is a long enough description of an issue.");

        mockMvc.perform(post("/api/tickets")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getTicket_ownerCanRetrieveIt() throws Exception {
        String token = registerAndLogin("customer" + System.nanoTime() + "@example.com");

        var body = Map.of("title", "Broken feature", "description", "The export button does nothing when clicked.",
                "category", "TECHNICAL");

        String createResponse = mockMvc.perform(post("/api/tickets")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        long ticketId = objectMapper.readTree(createResponse).get("id").asLong();

        mockMvc.perform(get("/api/tickets/" + ticketId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ticketId));
    }

    @Test
    void customer_cannotAccessSupportDashboardEndpoints() throws Exception {
        String token = registerAndLogin("customer" + System.nanoTime() + "@example.com");

        mockMvc.perform(get("/api/tickets").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/stats").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void customer_cannotUpdateTicketStatus() throws Exception {
        String token = registerAndLogin("customer" + System.nanoTime() + "@example.com");

        var body = Map.of("title", "Some issue", "description", "This description is long enough to pass validation.",
                "category", "OTHER");

        String createResponse = mockMvc.perform(post("/api/tickets")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        long ticketId = objectMapper.readTree(createResponse).get("id").asLong();

        mockMvc.perform(patch("/api/tickets/" + ticketId + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "RESOLVED"))))
                .andExpect(status().isForbidden());
    }

    /**
     * IDOR check (Phase 3/4 requirement): a customer must not be able to view
     * another customer's ticket just by guessing/incrementing its numeric id.
     */
    @Test
    void customer_cannotViewAnotherCustomersTicket_returns403() throws Exception {
        String ownerToken = registerAndLogin("owner" + System.nanoTime() + "@example.com");
        String otherToken = registerAndLogin("intruder" + System.nanoTime() + "@example.com");

        var body = Map.of("title", "Private issue", "description", "This is a private support request with details.",
                "category", "ACCOUNT");

        String createResponse = mockMvc.perform(post("/api/tickets")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        long ticketId = objectMapper.readTree(createResponse).get("id").asLong();

        // The owner can read it...
        mockMvc.perform(get("/api/tickets/" + ticketId).header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk());

        // ...but a different customer, authenticated with their own valid token,
        // must be blocked from reading it just by changing the id in the URL.
        mockMvc.perform(get("/api/tickets/" + ticketId).header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void customer_cannotAddInternalNoteToTicket_returns403() throws Exception {
        String token = registerAndLogin("customer" + System.nanoTime() + "@example.com");

        var body = Map.of("title", "Some issue", "description", "This description is long enough to pass validation.",
                "category", "OTHER");

        String createResponse = mockMvc.perform(post("/api/tickets")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        long ticketId = objectMapper.readTree(createResponse).get("id").asLong();

        mockMvc.perform(post("/api/tickets/" + ticketId + "/notes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("content", "Trying to add a note"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void addNote_tooLong_returns400NotServerError() throws Exception {
        // Regression test: TicketNoteRequest previously had no @Size limit, so an
        // over-long note would fail at the database layer (raising an opaque 500)
        // instead of being rejected cleanly with a 400 at the validation layer.
        //
        // Note: @Valid request-body validation runs while Spring MVC resolves the
        // controller method's arguments, which happens before the @PreAuthorize
        // check fires (that check wraps the method *invocation* itself). So this
        // is testable with a plain customer token: an oversized body is rejected
        // with 400 regardless of who's calling, without needing to bootstrap an
        // agent/admin account through the public API just for this test.
        String token = registerAndLogin("customer" + System.nanoTime() + "@example.com");

        var ticketBody = Map.of("title", "Some issue", "description", "This description is long enough to pass validation.",
                "category", "OTHER");
        String createResponse = mockMvc.perform(post("/api/tickets")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ticketBody)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long ticketId = objectMapper.readTree(createResponse).get("id").asLong();

        String tooLongContent = "x".repeat(2001);

        mockMvc.perform(post("/api/tickets/" + ticketId + "/notes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("content", tooLongContent))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void nonAdmin_cannotCreateAgentAccount_returns403() throws Exception {
        // A non-admin (a plain customer) must not be able to self-elevate into a
        // SUPPORT_AGENT account through the staff-creation endpoint.
        String customerToken = registerAndLogin("plain" + System.nanoTime() + "@example.com");
        var body = Map.of("name", "Wannabe Agent", "email", "agent" + System.nanoTime() + "@example.com",
                "password", "strongPassword1");

        mockMvc.perform(post("/api/users/agents")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isForbidden());
    }
}
