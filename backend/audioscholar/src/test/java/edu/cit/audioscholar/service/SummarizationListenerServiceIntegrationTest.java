package edu.cit.audioscholar.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.cit.audioscholar.model.AudioMetadata;
import edu.cit.audioscholar.model.ProcessingStatus;
import edu.cit.audioscholar.util.RobustTaskExecutor;

/**
 * Integration tests for SummarizationListenerService focusing on exception
 * handling. Tests verify that exceptions are properly handled and metadata is
 * updated correctly.
 */
@ExtendWith(MockitoExtension.class)
class SummarizationListenerServiceIntegrationTest {

	@Mock
	private FirebaseService firebaseService;

	@Mock
	private GeminiService geminiService;

	@Mock
	private NhostStorageService nhostStorageService;

	@Mock
	private SummaryService summaryService;

	@Mock
	private CacheManager cacheManager;

	@Mock
	private LearningMaterialRecommenderService recommenderService;

	@Mock
	private RecordingService recordingService;

	@Mock
	private ObjectMapper objectMapper;

	@Mock
	private Cache cache;

	@Mock
	private RabbitTemplate rabbitTemplate;

	@Mock
	private RobustTaskExecutor robustTaskExecutor;

	private SummarizationListenerService summarizationListenerService;

	@Captor
	private ArgumentCaptor<Map<String, Object>> metadataUpdateCaptor;

	private static final String METADATA_ID = "test-metadata-id";
	private static final String USER_ID = "test-user-id";
	private static final String MESSAGE_ID = "test-message-id";

	@BeforeEach
	void setUp() {
		// Manually create the service with mocked dependencies to avoid constructor
		// injection issues
		summarizationListenerService = new SummarizationListenerService(firebaseService, geminiService,
				nhostStorageService, summaryService, cacheManager, objectMapper, "src/test/resources", // tempDir
				recommenderService, recordingService, rabbitTemplate, robustTaskExecutor);
	}

	// ==================== SIMPLIFIED EXCEPTION HANDLING TESTS ====================

	private void setupRobustTaskExecutorMock() {
		// Mock RobustTaskExecutor to execute the task immediately
		doAnswer(invocation -> {
			Runnable task = invocation.getArgument(2);
			try {
				task.run();
			} catch (Exception e) {
				// Simulate exception handling by the executor (it would retry in real life)
			}
			return null;
		}).when(robustTaskExecutor).executeWithInfiniteRetry(anyString(), anyString(), any(Runnable.class));
	}

	@Test
	void testHandleSummarizationRequest_GeminiServiceThrowsException_AudioOnly() throws Exception {
		setupRobustTaskExecutorMock();
		// Given
		Map<String, String> message = createValidAudioOnlyMessage();
		AudioMetadata metadata = createAudioOnlyMetadata();

		// Mock FirebaseService behavior
		mockFirebaseService(metadata);

		// Mock GeminiService to throw an exception
		doThrow(new RuntimeException("EnhancedGeminiException: All retry attempts and model fallbacks exhausted"))
				.when(geminiService).generateTranscriptOnlySummary(anyString(), eq(METADATA_ID));

		// When
		summarizationListenerService.handleSummarizationRequest(message);

		// Then - Verify that metadata was updated with SUMMARIZING status
		verify(firebaseService, atLeastOnce()).updateData(eq("audioMetadata"), eq(METADATA_ID),
				metadataUpdateCaptor.capture());

		// Verify that SUMMARIZING update occurred
		boolean foundSummarizingUpdate = metadataUpdateCaptor.getAllValues().stream()
				.anyMatch(update -> ProcessingStatus.SUMMARIZING.name().equals(update.get("status")));

		assertTrue(foundSummarizingUpdate, "Should have updated status to SUMMARIZING");

		// Verify NO failure update occurred (due to infinite retry)
		boolean foundFailureUpdate = metadataUpdateCaptor.getAllValues().stream()
				.anyMatch(update -> ProcessingStatus.SUMMARY_FAILED.name().equals(update.get("status")));
		assertFalse(foundFailureUpdate, "Should NOT have updated status to SUMMARY_FAILED (infinite retry expected)");

		// Verify that recommendations were NOT triggered
		verify(recommenderService, never()).generateAndSaveRecommendations(any(), any(), any());
		verify(recordingService, never()).getRecordingById(anyString());
	}

	// ==================== HELPER METHODS ====================

	private Map<String, String> createValidAudioOnlyMessage() {
		Map<String, String> message = new HashMap<>();
		message.put("metadataId", METADATA_ID);
		message.put("messageId", MESSAGE_ID);
		return message;
	}

	private AudioMetadata createAudioOnlyMetadata() {
		AudioMetadata metadata = new AudioMetadata();
		metadata.setId(METADATA_ID);
		metadata.setUserId(USER_ID);
		metadata.setStatus(ProcessingStatus.SUMMARIZATION_QUEUED);
		metadata.setTranscriptText("Test transcript text");
		metadata.setAudioOnly(true);
		return metadata;
	}

	private void mockFirebaseService(AudioMetadata metadata) {
		try {
			// Mock the method call that gets the collection name
			doReturn("audioMetadata").when(firebaseService).getAudioMetadataCollectionName();
		} catch (Exception e) {
			// If the method doesn't exist, just return a default value
		}

		// Mock the actual data retrieval - handle null case
		if (metadata != null) {
			// Create a proper map with String values for testing
			Map<String, Object> metadataMap = new HashMap<>();
			metadataMap.put("id", metadata.getId());
			metadataMap.put("userId", metadata.getUserId());
			metadataMap.put("status", metadata.getStatus().name());
			metadataMap.put("transcriptText", metadata.getTranscriptText());
			metadataMap.put("audioOnly", metadata.isAudioOnly());
			metadataMap.put("failureReason", null);
			// Ensure all values are strings or primitives, not Firestore FieldValues

			doReturn(metadataMap).when(firebaseService).getData(eq("audioMetadata"), eq(METADATA_ID));
		} else {
			doReturn(null).when(firebaseService).getData(eq("audioMetadata"), eq(METADATA_ID));
		}
	}

	@Test
	void testHandleSummarizationRequest_NullMessageHandling() {
		// When
		summarizationListenerService.handleSummarizationRequest(null);

		// Then - Should not throw exception, just log error and return
		verify(firebaseService, never()).getData(any(), any());
		verify(firebaseService, never()).updateData(any(), any(), any());
	}

	@Test
	void testHandleSummarizationRequest_InvalidMetadataId() {
		// Given
		Map<String, String> message = new HashMap<>();
		message.put("metadataId", "");
		message.put("messageId", MESSAGE_ID);

		// When
		summarizationListenerService.handleSummarizationRequest(message);

		// Then - Should handle gracefully
		verify(firebaseService, never()).getData(any(), any());
	}

	@Test
	void testHandleSummarizationRequest_MetadataNotFound() {
		setupRobustTaskExecutorMock();
		// Given
		Map<String, String> message = createValidAudioOnlyMessage();

		mockFirebaseService(null); // Return null for metadata

		// When
		summarizationListenerService.handleSummarizationRequest(message);

		// Then - Should handle gracefully and return early
		verify(firebaseService).getData(eq("audioMetadata"), eq(METADATA_ID));
		verify(geminiService, never()).generateTranscriptOnlySummary(any(), any());
		verify(firebaseService, never()).updateData(any(), any(), any());
	}
}
