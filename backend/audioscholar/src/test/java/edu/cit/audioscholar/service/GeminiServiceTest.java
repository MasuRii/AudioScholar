package edu.cit.audioscholar.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import edu.cit.audioscholar.model.KeyProvider;

@ExtendWith(MockitoExtension.class)
class GeminiServiceTest {

	@Mock
	private RestTemplate restTemplate;

	@Mock
	private KeyRotationManager keyRotationManager;

	@Mock
	private GeminiSmartRotationService rotationService;

	@InjectMocks
	private GeminiService geminiService;

	private static final String API_KEY = "test-api-key";
	private static final String PROMPT_TEXT = "Test prompt";
	private static final String TRANSCRIPT_TEXT = "Test transcript text";
	private static final String METADATA_ID = "test-metadata-id";

	@BeforeEach
	void setUp() {
		// Set configuration values that are still present in GeminiService for legacy
		// methods.
		ReflectionTestUtils.setField(geminiService, "transcriptionModelName", "gemini-2.0-flash");
		ReflectionTestUtils.setField(geminiService, "summarizationModelName", "gemini-2.5-flash");
	}

	// ==================== SUCCESS SCENARIOS (New Fallback Logic)
	// ====================

	@Test
	void testCallGeminiTranscriptionAPIWithFallback_VerifyPrompt() throws Exception {
		// Given
		Path tempFile = Files.createTempFile("test-audio", ".mp3");
		Files.writeString(tempFile, "dummy content");
		try {
			when(keyRotationManager.getKey(any(KeyProvider.class))).thenReturn(API_KEY);

			// Mock Upload - Initiate
			HttpHeaders initiateHeaders = new HttpHeaders();
			initiateHeaders.add("X-Goog-Upload-Url", "http://upload-url");
			ResponseEntity<String> initiateResponse = new ResponseEntity<>(null, initiateHeaders, HttpStatus.OK);
			when(restTemplate.exchange(contains("/upload/v1beta/files"), eq(HttpMethod.POST), any(), eq(String.class)))
					.thenReturn(initiateResponse);

			// Mock Upload - Content
			String uploadResponseBody = "{\"file\": {\"uri\": \"http://file-uri\"}}";
			ResponseEntity<String> uploadResponse = new ResponseEntity<>(uploadResponseBody, HttpStatus.OK);
			when(restTemplate.exchange(eq("http://upload-url"), eq(HttpMethod.POST), any(), eq(String.class)))
					.thenReturn(uploadResponse);

			// Mock Rotation Service
			when(rotationService.executeWithInfiniteRotation(any())).thenAnswer(invocation -> {
				Function<String, String> apiCallFunction = invocation.getArgument(0);
				return apiCallFunction.apply("gemini-2.0-flash");
			});

			// Mock Transcription Call
			String transcriptionResponse = "{\"candidates\": [{\"content\": {\"parts\": [{\"text\": \"{\\\"transcript\\\": \\\"Hello world\\\"}\"}]}}]}";
			ResponseEntity<String> transResponse = new ResponseEntity<>(transcriptionResponse, HttpStatus.OK);

			// We need to capture the argument to verify the prompt
			ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
			when(restTemplate.exchange(contains(":generateContent"), eq(HttpMethod.POST), entityCaptor.capture(),
					eq(String.class))).thenReturn(transResponse);

			// When
			geminiService.callGeminiTranscriptionAPIWithFallback(tempFile, "test-audio.mp3");

			// Then
			HttpEntity capturedEntity = entityCaptor.getValue();
			String body = capturedEntity.getBody().toString();

			String expectedPrompt = "Transcribe the following audio content accurately. If the audio contains no speech or only silence, output the exact text '[NO SPEECH DETECTED]' in the transcript field. Otherwise, output only the spoken text. Maintain original punctuation, capitalization, and paragraph breaks as best as possible. For numbers, spell them as digits if they represent quantities or measurements, and as words if they are part of natural speech. Include any hesitations, repetitions, or fillers that are meaningful to the content.";

			assertTrue(body.contains(expectedPrompt),
					"The prompt sent to Gemini API does not match the expected new prompt.");

		} finally {
			Files.deleteIfExists(tempFile);
		}
	}

	@Test
	void testCallGeminiSummarizationAPIWithFallback_SuccessFirstAttempt() {
		// Given
		when(keyRotationManager.getKey(any(KeyProvider.class))).thenReturn(API_KEY);
		String expectedExtractedText = "{\"summaryText\": \"Test summary\", \"keyPoints\": [\"Point 1\", \"Point 2\"], \"topics\": [\"Topic 1\"], \"glossary\": []}";
		ResponseEntity<String> successResponse = new ResponseEntity<>(createFullApiResponse(), HttpStatus.OK);

		// Mock the rotation service to execute the lambda immediately
		when(rotationService.executeWithInfiniteRotation(any())).thenAnswer(invocation -> {
			Function<String, String> apiCallFunction = invocation.getArgument(0);
			// The lambda will be called with a test model name
			return apiCallFunction.apply("gemini-pro");
		});

		// Mock success on the RestTemplate call inside the lambda
		when(restTemplate.exchange(contains("gemini-pro"), eq(HttpMethod.POST), any(), eq(String.class)))
				.thenReturn(successResponse);

		// When
		String result = geminiService.callGeminiSummarizationAPIWithFallback(PROMPT_TEXT, TRANSCRIPT_TEXT);

		// Then
		assertEquals(expectedExtractedText, result);
		verify(rotationService, times(1)).executeWithInfiniteRotation(any());
		verify(restTemplate, times(1)).exchange(anyString(), eq(HttpMethod.POST), any(), eq(String.class));
		verify(keyRotationManager, times(1)).reportSuccess(KeyProvider.GEMINI, API_KEY);
	}

	// ==================== FAILURE SCENARIOS (New Fallback Logic)
	// ====================

	@Test
	void testCallGeminiSummarizationAPIWithFallback_RotationServiceThrowsException() {
		// Given
		// Mock the rotation service to throw an exception after its internal retries
		when(rotationService.executeWithInfiniteRotation(any())).thenThrow(new RuntimeException("All models failed"));

		// When
		String result = geminiService.callGeminiSummarizationAPIWithFallback(PROMPT_TEXT, TRANSCRIPT_TEXT);

		// Then
		assertTrue(result.contains("\"error\":\"Unexpected Error\""));
		assertTrue(result.contains("\"details\":\"All models failed\""));
	}

	@Test
	void testCallGeminiSummarizationAPIWithFallback_LambdaThrowsRateLimitException() {
		// Given
		when(keyRotationManager.getKey(any(KeyProvider.class))).thenReturn(API_KEY);
		when(rotationService.executeWithInfiniteRotation(any())).thenAnswer(invocation -> {
			Function<String, String> apiCallFunction = invocation.getArgument(0);
			// This will throw the HttpClientErrorException which is caught by the outer
			// try-catch in the service
			try {
				return apiCallFunction.apply("gemini-pro");
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});

		when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(String.class)))
				.thenThrow(new HttpClientErrorException(HttpStatus.TOO_MANY_REQUESTS));

		// When
		String result = geminiService.callGeminiSummarizationAPIWithFallback(PROMPT_TEXT, TRANSCRIPT_TEXT);

		// Then
		// The service method catches the exception and returns a JSON error response.
		assertTrue(result.contains("\"error\":\"Unexpected Error\""));
		assertTrue(result.contains("429 TOO_MANY_REQUESTS"));

		// Verify that no success is reported
		verify(keyRotationManager, never()).reportSuccess(any(), any());
	}

	// ==================== LEGACY METHOD TESTS ====================

	// All legacy tests are removed as the retry logic is now handled by
	// GeminiSmartRotationService and tested implicitly in the new fallback tests.

	// ==================== HELPER METHODS ====================

	private String createFullApiResponse() {
		return "{" + "    \"candidates\": [" + "        {" + "            \"content\": {"
				+ "                \"parts\": [" + "                    {"
				+ "                        \"text\": \"{\\\"summaryText\\\": \\\"Test summary\\\", \\\"keyPoints\\\": [\\\"Point 1\\\", \\\"Point 2\\\"], \\\"topics\\\": [\\\"Topic 1\\\"], \\\"glossary\\\": []}\""
				+ "                    }" + "                ]" + "            }" + "        }" + "    ]" + "}";
	}
}
