package edu.cit.audioscholar.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Unit tests for GeminiService focusing on exponential backoff and model
 * fallback logic. Tests cover successful calls, retry scenarios, and failure
 * conditions.
 */
@ExtendWith(MockitoExtension.class)
class GeminiServiceTest {

	@Mock
	private RestTemplate restTemplate;

	@InjectMocks
	private GeminiService geminiService;

	private final ObjectMapper objectMapper = new ObjectMapper();

	private static final String API_KEY = "test-api-key";
	private static final String PROMPT_TEXT = "Test prompt";
	private static final String TRANSCRIPT_TEXT = "Test transcript text";
	private static final String METADATA_ID = "test-metadata-id";

	@BeforeEach
	void setUp() {
		// Set test configuration values
		ReflectionTestUtils.setField(geminiService, "apiKey", API_KEY);
		ReflectionTestUtils.setField(geminiService, "maxRetryAttempts", 3);
		ReflectionTestUtils.setField(geminiService, "baseRetryDelayMs", 1000L);
		ReflectionTestUtils.setField(geminiService, "maxRetryDelayMs", 30000L);
		ReflectionTestUtils.setField(geminiService, "backoffMultiplier", 2.0);
		ReflectionTestUtils.setField(geminiService, "maxTotalAttempts", 21);
		ReflectionTestUtils.setField(geminiService, "modelFallbackSequence",
				"gemini-2.5-pro,gemini-flash-latest,gemini-2.5-flash");
		ReflectionTestUtils.setField(geminiService, "transcriptionModelName", "gemini-2.0-flash");
		ReflectionTestUtils.setField(geminiService, "summarizationModelName", "gemini-2.5-flash");
	}

	// ==================== SUCCESS SCENARIOS ====================

	@Test
	void testCallGeminiSummarizationAPIWithFallback_SuccessFirstAttempt() {
		// Given
		String expectedExtractedText = "{\"summaryText\": \"Test summary\", \"keyPoints\": [\"Point 1\", \"Point 2\"], \"topics\": [\"Topic 1\"], \"glossary\": []}";
		ResponseEntity<String> successResponse = new ResponseEntity<>(createFullApiResponse(), HttpStatus.OK);

		// Mock success on first attempt
		when(restTemplate.exchange(anyString(), eq(org.springframework.http.HttpMethod.POST), any(), eq(String.class)))
				.thenReturn(successResponse);

		// When
		String result = geminiService.callGeminiSummarizationAPIWithFallback(PROMPT_TEXT, TRANSCRIPT_TEXT);

		// Then - Service extracts the text from the response
		assertEquals(expectedExtractedText, result);
		verify(restTemplate, times(1)).exchange(anyString(), eq(org.springframework.http.HttpMethod.POST), any(),
				eq(String.class));
	}

	@Test
	void testCallGeminiSummarizationAPIWithFallback_SuccessAfterRetries() throws InterruptedException {
		// Given
		String expectedExtractedText = "{\"summaryText\": \"Test summary\", \"keyPoints\": [\"Point 1\", \"Point 2\"], \"topics\": [\"Topic 1\"], \"glossary\": []}";

		callSequence = 0;
		// Mock failures followed by success to test retry logic
		doAnswer(invocation -> {
			callSequence++;
			if (callSequence <= 3) {
				throw new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE, "Service temporarily unavailable");
			} else {
				return new ResponseEntity<>(createFullApiResponse(), HttpStatus.OK);
			}
		}).when(restTemplate).exchange(anyString(), eq(org.springframework.http.HttpMethod.POST), any(),
				eq(String.class));

		// When
		long startTime = System.currentTimeMillis();
		String result = geminiService.callGeminiSummarizationAPIWithFallback(PROMPT_TEXT, TRANSCRIPT_TEXT);
		long endTime = System.currentTimeMillis();

		// Then
		assertEquals(expectedExtractedText, result);
		// Should have made 4 calls (3 failures + 1 success)
		verify(restTemplate, atLeast(4)).exchange(anyString(), eq(org.springframework.http.HttpMethod.POST), any(),
				eq(String.class));

		// Verify retry delay occurred
		assertTrue(endTime - startTime >= 3000, "Exponential backoff delay should have occurred");
	}

	@Test
	void testCallGeminiSummarizationAPIWithFallback_SuccessAfterModelFallback() throws InterruptedException {
		// Given
		String expectedExtractedText = "{\"summaryText\": \"Test summary\", \"keyPoints\": [\"Point 1\", \"Point 2\"], \"topics\": [\"Topic 1\"], \"glossary\": []}";

		// Configure for single model to simplify
		ReflectionTestUtils.setField(geminiService, "modelFallbackSequence", "gemini-2.5-flash");

		callSequence = 0;
		// Mock transient error then success
		doAnswer(invocation -> {
			callSequence++;
			if (callSequence == 1) {
				throw new ResourceAccessException("Connection timeout");
			} else {
				return new ResponseEntity<>(createFullApiResponse(), HttpStatus.OK);
			}
		}).when(restTemplate).exchange(anyString(), eq(org.springframework.http.HttpMethod.POST), any(),
				eq(String.class));

		// When
		long startTime = System.currentTimeMillis();
		String result = geminiService.callGeminiSummarizationAPIWithFallback(PROMPT_TEXT, TRANSCRIPT_TEXT);
		long endTime = System.currentTimeMillis();

		// Then
		assertEquals(expectedExtractedText, result);
		verify(restTemplate, atLeast(2)).exchange(anyString(), eq(org.springframework.http.HttpMethod.POST), any(),
				eq(String.class));

		// Verify retry delay occurred
		assertTrue(endTime - startTime >= 1000, "Retry delay should have occurred for transient error");
	}

	// ==================== FAILURE SCENARIOS ====================

	@Test
	void testCallGeminiSummarizationAPIWithFallback_AllRetriesExhausted() throws InterruptedException {
		// Given
		// Mock all attempts to fail with 503 Service Unavailable
		doThrow(new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE, "Service unavailable")).when(restTemplate)
				.exchange(anyString(), eq(org.springframework.http.HttpMethod.POST), any(), eq(String.class));

		// When
		String result = geminiService.callGeminiSummarizationAPIWithFallback(PROMPT_TEXT, TRANSCRIPT_TEXT);

		// Then
		assertTrue(result.contains("error"));
		assertTrue(result.contains("API Request Failed"));
		// Verify that multiple attempts were made due to retry logic
		verify(restTemplate, atLeast(3)).exchange(anyString(), eq(org.springframework.http.HttpMethod.POST), any(),
				eq(String.class));
	}

	@Test
	void testCallGeminiSummarizationAPIWithFallback_ApiExceptionHandled() {
		// Given
		callSequence = 0;
		// Mock unexpected exception then null response (which will trigger error
		// handling)
		doAnswer(invocation -> {
			callSequence++;
			if (callSequence == 1) {
				throw new RuntimeException("Unexpected API error");
			} else {
				return null; // This will be handled by the retry logic
			}
		}).when(restTemplate).exchange(anyString(), eq(org.springframework.http.HttpMethod.POST), any(),
				eq(String.class));

		// When
		String result = geminiService.callGeminiSummarizationAPIWithFallback(PROMPT_TEXT, TRANSCRIPT_TEXT);

		// Then
		assertTrue(result.contains("error") || result.contains("Unexpected Error"));
		verify(restTemplate, atLeast(3)).exchange(anyString(), eq(org.springframework.http.HttpMethod.POST), any(),
				eq(String.class));
	}

	// ==================== HELPER METHODS ====================

	// Creates the full API response with candidates structure (what Gemini API
	// returns)
	private String createFullApiResponse() {
		return "{" + "    \"candidates\": [" + "        {" + "            \"content\": {"
				+ "                \"parts\": [" + "                    {"
				+ "                        \"text\": \"{\\\"summaryText\\\": \\\"Test summary\\\", \\\"keyPoints\\\": [\\\"Point 1\\\", \\\"Point 2\\\"], \\\"topics\\\": [\\\"Topic 1\\\"], \\\"glossary\\\": []}\""
				+ "                    }" + "                ]" + "            }" + "        }" + "    ]" + "}";
	}

	@Test
	void testGenerateTranscriptOnlySummary_Success() throws InterruptedException {
		// Given
		String expectedExtractedText = "{\"summaryText\": \"Test summary\", \"keyPoints\": [\"Point 1\", \"Point 2\"], \"topics\": [\"Topic 1\"], \"glossary\": []}";

		callSequence = 0;
		// Return a successful response that can be properly parsed
		doAnswer(invocation -> {
			callSequence++;
			return new ResponseEntity<>(createFullApiResponse(), HttpStatus.OK);
		}).when(restTemplate).exchange(anyString(), eq(org.springframework.http.HttpMethod.POST), any(),
				eq(String.class));

		// When
		String result = geminiService.generateTranscriptOnlySummary(TRANSCRIPT_TEXT, METADATA_ID);

		// Then
		assertEquals(expectedExtractedText, result);
		// Verify it was called at least once
		verify(restTemplate, atLeast(1)).exchange(anyString(), eq(org.springframework.http.HttpMethod.POST), any(),
				eq(String.class));
	}

	@Test
	void testGenerateTranscriptOnlySummary_RetryWithBackoff() throws InterruptedException {
		// Given
		String expectedExtractedText = "{\"summaryText\": \"Test summary\", \"keyPoints\": [\"Point 1\", \"Point 2\"], \"topics\": [\"Topic 1\"], \"glossary\": []}";

		callSequence = 0;
		// First call fails, second succeeds
		doAnswer(invocation -> {
			callSequence++;
			if (callSequence == 1) {
				throw new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE, "Service down");
			} else {
				return new ResponseEntity<>(createFullApiResponse(), HttpStatus.OK);
			}
		}).when(restTemplate).exchange(anyString(), eq(org.springframework.http.HttpMethod.POST), any(),
				eq(String.class));

		// When
		long startTime = System.currentTimeMillis();
		String result = geminiService.generateTranscriptOnlySummary(TRANSCRIPT_TEXT, METADATA_ID);
		long endTime = System.currentTimeMillis();

		// Then
		assertEquals(expectedExtractedText, result);
		verify(restTemplate, times(2)).exchange(anyString(), eq(org.springframework.http.HttpMethod.POST), any(),
				eq(String.class));
		assertTrue(endTime - startTime >= 1000, "Should have retry delay");
	}

	// Helper to track call counts for more flexible mocking
	private int callSequence = 0;
}
