package com.aiincident.service;

import com.aiincident.model.TicketCategory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Covers the AI layer's failure/fallback behavior (Phase 5/8 requirement): the
 * real Anthropic-backed implementation must never let a bad network response,
 * a malformed model output, or missing configuration crash ticket creation -
 * it should always degrade to the mock provider instead.
 */
@ExtendWith(MockitoExtension.class)
class AnthropicAIAnalysisServiceTest {

    @Mock
    private RestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MockAIAnalysisService mockFallback = new MockAIAnalysisService();

    private AnthropicAIAnalysisService buildService(String provider, String apiKey) {
        return new AnthropicAIAnalysisService(
                restTemplate, objectMapper, mockFallback,
                apiKey, "https://api.anthropic.com/v1/messages",
                "claude-3-5-haiku-20241022", provider);
    }

    private void mockAnthropicTextResponse(String modelText) {
        try {
            String responseJson = """
                    {"content":[{"type":"text","text":%s}]}
                    """.formatted(objectMapper.valueToTree(modelText).toString());
            JsonNode node = objectMapper.readTree(responseJson);
            when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(JsonNode.class)))
                    .thenReturn(ResponseEntity.ok(node));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void analyze_providerNotAnthropic_usesMockWithoutCallingRestTemplate() {
        AnthropicAIAnalysisService service = buildService("mock", "sk-ant-shouldnotbeused");

        AIAnalysisResult result = service.analyze("Title", "A description long enough to analyze.", TicketCategory.OTHER);

        assertThat(result.analyzedBy()).contains("mock");
        verifyNoInteractions(restTemplate);
    }

    @Test
    void analyze_noApiKeyConfigured_usesMockWithoutCallingRestTemplate() {
        AnthropicAIAnalysisService service = buildService("anthropic", "");

        AIAnalysisResult result = service.analyze("Title", "A description long enough to analyze.", TicketCategory.OTHER);

        assertThat(result.analyzedBy()).contains("mock");
        verifyNoInteractions(restTemplate);
    }

    @Test
    void analyze_validAnthropicResponse_parsesStructuredAnalysis() {
        AnthropicAIAnalysisService service = buildService("anthropic", "sk-ant-fake-key");
        mockAnthropicTextResponse("""
                {"category":"PAYMENT","severity":"HIGH","sentiment":"NEGATIVE",\
                "emotionalIntensity":8,"priority":"HIGH","summary":"Duplicate charge.",\
                "suggestedResolution":"Refund the duplicate charge."}""");

        AIAnalysisResult result = service.analyze("Duplicate charge", "I was charged twice.", TicketCategory.PAYMENT);

        assertThat(result.category()).isEqualTo(TicketCategory.PAYMENT);
        assertThat(result.emotionalIntensity()).isEqualTo(8);
        assertThat(result.analyzedBy()).startsWith("anthropic:");
    }

    @Test
    void analyze_malformedJson_fallsBackToMockInsteadOfThrowing() {
        AnthropicAIAnalysisService service = buildService("anthropic", "sk-ant-fake-key");
        mockAnthropicTextResponse("this is not JSON at all, sorry!");

        AIAnalysisResult result = service.analyze("Title", "A description long enough to analyze.", TicketCategory.OTHER);

        assertThat(result.analyzedBy()).contains("mock");
    }

    @Test
    void analyze_validJsonButInvalidEnumValue_fallsBackToMockInsteadOfThrowing() {
        // The model returned syntactically valid JSON, but "category" isn't one of
        // our six known TicketCategory values - this must not propagate an
        // IllegalArgumentException out of ticket creation.
        AnthropicAIAnalysisService service = buildService("anthropic", "sk-ant-fake-key");
        mockAnthropicTextResponse("""
                {"category":"BILLING_DISPUTE","severity":"HIGH","sentiment":"NEGATIVE",\
                "emotionalIntensity":8,"priority":"HIGH","summary":"x","suggestedResolution":"y"}""");

        AIAnalysisResult result = service.analyze("Title", "A description long enough to analyze.", TicketCategory.OTHER);

        assertThat(result.analyzedBy()).contains("mock");
    }

    @Test
    void analyze_noJsonObjectInResponseText_fallsBackToMockInsteadOfThrowing() {
        AnthropicAIAnalysisService service = buildService("anthropic", "sk-ant-fake-key");
        mockAnthropicTextResponse("Sure, here is my analysis in plain prose with no JSON at all.");

        AIAnalysisResult result = service.analyze("Title", "A description long enough to analyze.", TicketCategory.OTHER);

        assertThat(result.analyzedBy()).contains("mock");
    }

    @Test
    void analyze_networkFailure_fallsBackToMockInsteadOfThrowing() {
        AnthropicAIAnalysisService service = buildService("anthropic", "sk-ant-fake-key");
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(JsonNode.class)))
                .thenThrow(new RestClientException("connection refused"));

        AIAnalysisResult result = service.analyze("Title", "A description long enough to analyze.", TicketCategory.OTHER);

        assertThat(result.analyzedBy()).contains("mock");
    }
}
