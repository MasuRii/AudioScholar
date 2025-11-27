package edu.cit.audioscholar.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.google.api.core.ApiFuture;
import com.google.api.services.youtube.model.ResourceId;
import com.google.api.services.youtube.model.SearchResult;
import com.google.api.services.youtube.model.SearchResultSnippet;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteBatch;
import com.google.cloud.firestore.WriteResult;
import com.google.common.base.Optional;
import com.google.firebase.messaging.MulticastMessage;
import com.optimaize.langdetect.LanguageDetector;
import com.optimaize.langdetect.LanguageDetectorBuilder;
import com.optimaize.langdetect.i18n.LdLocale;
import com.optimaize.langdetect.ngram.NgramExtractors;
import com.optimaize.langdetect.profiles.LanguageProfile;
import com.optimaize.langdetect.profiles.LanguageProfileReader;
import com.optimaize.langdetect.text.CommonTextObjectFactories;
import com.optimaize.langdetect.text.TextObject;
import com.optimaize.langdetect.text.TextObjectFactory;

import edu.cit.audioscholar.dto.AnalysisResults;
import edu.cit.audioscholar.integration.YouTubeAPIClient;
import edu.cit.audioscholar.model.LearningRecommendation;
import edu.cit.audioscholar.model.ProcessingStatus;
import edu.cit.audioscholar.model.Recording;
import edu.cit.audioscholar.util.RobustTaskExecutor;
import jakarta.annotation.PostConstruct;

@Service
public class LearningMaterialRecommenderService {
	private static final Logger log = LoggerFactory.getLogger(LearningMaterialRecommenderService.class);
	private final LectureContentAnalyzerService lectureContentAnalyzerService;
	private final YouTubeAPIClient youTubeAPIClient;
	private final Firestore firestore;
	private final RecordingService recordingService;
	private final FirebaseService firebaseService;
	private final UserService userService;
	private final String recommendationsCollectionName;
	private final RobustTaskExecutor robustTaskExecutor;
	private static final int MAX_RECOMMENDATIONS_TO_FETCH = 10;
	private static final int SEARCH_RESULTS_POOL_SIZE = 50;
	private static final Set<String> EDUCATIONAL_DOMAINS = Set.of("edu", "education", "academic", "university",
			"school", "college", "course");

	private LanguageDetector languageDetector;
	private TextObjectFactory textObjectFactory;

	public LearningMaterialRecommenderService(LectureContentAnalyzerService lectureContentAnalyzerService,
			YouTubeAPIClient youTubeAPIClient, Firestore firestore, RecordingService recordingService,
			FirebaseService firebaseService, UserService userService,
			@Value("${firebase.firestore.collection.recommendations}") String recommendationsCollectionName,
			RobustTaskExecutor robustTaskExecutor) {
		this.lectureContentAnalyzerService = lectureContentAnalyzerService;
		this.youTubeAPIClient = youTubeAPIClient;
		this.firestore = firestore;
		this.recordingService = recordingService;
		this.firebaseService = firebaseService;
		this.userService = userService;
		this.recommendationsCollectionName = recommendationsCollectionName;
		this.robustTaskExecutor = robustTaskExecutor;
	}

	@PostConstruct
	public void initLanguageDetector() {
		try {
			List<LanguageProfile> languageProfiles = new LanguageProfileReader().readAllBuiltIn();
			languageDetector = LanguageDetectorBuilder.create(NgramExtractors.standard()).withProfiles(languageProfiles)
					.build();
			textObjectFactory = CommonTextObjectFactories.forDetectingOnLargeText();
			log.info("Language Detector initialized successfully.");
		} catch (IOException e) {
			log.error("Failed to initialize Language Detector", e);
		}
	}

	public List<LearningRecommendation> generateAndSaveRecommendations(String userId, String recordingId,
			String summaryId) {
		log.info("Starting recommendation generation and storage for recording ID: {}, user: {}, summary: {}",
				recordingId, userId, summaryId);
		AnalysisResults analysisResults = lectureContentAnalyzerService.analyzeLectureContent(recordingId);
		if (!analysisResults.isSuccess()) {
			log.error("Failed to analyze lecture content for recording ID: {}. Error: {}", recordingId,
					analysisResults.getErrorMessage());
			return Collections.emptyList();
		}
		List<String> searchQueries = analysisResults.getKeywordsAndTopics();
		if (searchQueries.isEmpty()) {
			log.info("No search queries found for recording ID: {}. Cannot generate recommendations.", recordingId);
			return Collections.emptyList();
		}
		log.debug("Search queries extracted for recording ID {}: {}", recordingId, searchQueries);

		// Queries are now intent-based and complete, so we use them directly.
		log.debug("Using search queries for recording ID {}: {}", recordingId, searchQueries);

		try {
			List<SearchResult> youtubeResults = robustTaskExecutor.executeWithInfiniteRetry(recordingId,
					"searching YouTube videos",
					() -> youTubeAPIClient.searchVideos(searchQueries, SEARCH_RESULTS_POOL_SIZE));

			if (youtubeResults.isEmpty()) {
				log.info(
						"YouTube search returned no results (or failed) for keywords related to recording ID: {}. Proceeding with empty recommendations.",
						recordingId);
				// Instead of returning empty list immediately which might be interpreted as "no
				// recs found",
				// we should consider if we need to notify about "Completed with Warnings" here.
				// But since this method returns a List, we return empty list.
				// The caller (SummarizationListenerService) needs to decide status based on
				// this list or exception.
				// However, to support "COMPLETED_WITH_WARNINGS", we need to signal this
				// failure.
				// But currently, returning empty list is valid.
				// Let's try to update status directly if we can, or rely on the fact that empty
				// list means no recs.

				// If we failed due to API block, we should probably still mark as "Complete"
				// (or Complete with warnings)
				// rather than "Failed".

				// We will return empty list, but maybe we should update status to
				// COMPLETED_WITH_WARNINGS here?
				// The caller (SummarizationListenerService) calls updateMetadataStatus(...,
				// COMPLETE, null) AFTER this method returns successfully.
				// So if we return empty list, it will mark as COMPLETE.
				// If we want COMPLETED_WITH_WARNINGS, we might need to change return type or
				// update status here.

				// Let's update status here if it's a failure case that we caught.
				// But we don't have easy access to updateMetadataStatus here without
				// duplicating logic.
				// Best approach: Just return empty list. The system will mark as COMPLETE.
				// "Completed with warnings" is better, but "Complete" (with 0 recs) is
				// acceptable fallback for now to ensure flow finishes.
				// Wait, the requirements say: "Update Firestore status to
				// COMPLETED_WITH_WARNINGS instead of FAILED."

				// To do this properly, we need to update the status.
				updateStatusToCompletedWithWarnings(recordingId);

				// Still notify user even if no recs?
				// linkRecommendationsAndNotify handles the "success" notification.
				// If list is empty, it won't call linkRecommendationsAndNotify (based on
				// current logic it returns empty list).
				// So we should manually trigger notification saying "Processing Complete" (even
				// without recs).
				notifyProcessingComplete(userId, recordingId, summaryId);

				return Collections.emptyList();
			}

			log.info("Retrieved {} potential recommendations from YouTube for recording ID: {}", youtubeResults.size(),
					recordingId);

			List<SearchResult> filteredResults = filterAndRankResults(youtubeResults, searchQueries);

			List<SearchResult> languageFilteredResults = filterByLanguage(filteredResults);
			log.info("Filtered to {} relevant results after language filtering for recording ID: {}",
					languageFilteredResults.size(), recordingId);

			List<SearchResult> topResults = languageFilteredResults.stream().limit(MAX_RECOMMENDATIONS_TO_FETCH)
					.collect(Collectors.toList());

			List<LearningRecommendation> recommendations = processYouTubeResults(topResults, recordingId);
			if (recommendations.isEmpty()) {
				log.info("No valid recommendations processed for recording ID: {}", recordingId);
				notifyProcessingComplete(userId, recordingId, summaryId);
				return Collections.emptyList();
			}
			log.info("Successfully processed {} unique recommendations for recording ID: {}", recommendations.size(),
					recordingId);
			List<LearningRecommendation> savedRecommendationsWithIds = saveRecommendationsBatch(recommendations);
			if (!savedRecommendationsWithIds.isEmpty()) {
				linkRecommendationsAndNotify(userId, recordingId, summaryId, savedRecommendationsWithIds);
			} else {
				log.warn("No recommendations were successfully saved for recording ID: {}. Skipping linking step.",
						recordingId);
				notifyProcessingComplete(userId, recordingId, summaryId);
			}
			return savedRecommendationsWithIds;
		} catch (Exception e) {
			log.error("Unexpected error during recommendation generation or saving for recording ID: {}", recordingId,
					e);
			if (e instanceof InterruptedException) {
				Thread.currentThread().interrupt();
			}
			// Fallback: Mark as completed with warnings instead of failing entire flow
			updateStatusToCompletedWithWarnings(recordingId);
			notifyProcessingComplete(userId, recordingId, summaryId);
			return Collections.emptyList();
		}
	}

	private void updateStatusToCompletedWithWarnings(String recordingId) {
		try {
			// We need metadataId to update status. recordingId IS metadataId in this
			// context usually.
			// Let's assume recordingId == metadataId (which is true in this app structure)
			Map<String, Object> updates = new HashMap<>();
			updates.put("status", ProcessingStatus.COMPLETED_WITH_WARNINGS.name());
			updates.put("lastUpdated", new Date()); // Using Date here, Firestore handles it
			firestore.collection("audio_metadata").document(Objects.requireNonNull(recordingId)).update(updates);
			log.info("Updated status to COMPLETED_WITH_WARNINGS for recording ID: {}", recordingId);
		} catch (Exception e) {
			log.error("Failed to update status to COMPLETED_WITH_WARNINGS for recording ID: {}", recordingId, e);
		}
	}

	private void notifyProcessingComplete(String userId, String recordingId, String summaryId) {
		// If we have no recommendations, we still want to notify user that processing
		// is done.
		// We can reuse logic from linkRecommendationsAndNotify but skip the linking
		// part.
		log.info("Sending completion notification (without recommendations) for user: {}, recording: {}", userId,
				recordingId);
		try {
			List<String> tokensToSend = userService.getFcmTokensForUser(userId);
			if (tokensToSend != null && !tokensToSend.isEmpty()) {
				MulticastMessage message = firebaseService.buildProcessingCompleteMessage(userId, recordingId,
						summaryId);
				if (message != null) {
					firebaseService.sendFcmMessage(message, tokensToSend, userId);
				}
			}
		} catch (Exception e) {
			log.warn("Failed to send completion notification: {}", e.getMessage());
		}
	}

	private List<LearningRecommendation> saveRecommendationsBatch(List<LearningRecommendation> recommendations) {
		if (recommendations == null || recommendations.isEmpty()) {
			return Collections.emptyList();
		}
		String recordingId = recommendations.get(0).getRecordingId();
		List<LearningRecommendation> recommendationsWithIds = new ArrayList<>();
		try {
			WriteBatch batch = firestore.batch();
			for (LearningRecommendation recommendation : recommendations) {
				DocumentReference docRef = firestore.collection(Objects.requireNonNull(recommendationsCollectionName))
						.document();
				recommendation.setRecommendationId(docRef.getId());
				batch.set(docRef, recommendation);
				recommendationsWithIds.add(recommendation);
			}
			log.info("Attempting to commit batch write of {} recommendations to Firestore for recording ID: {}",
					recommendationsWithIds.size(), recordingId);
			ApiFuture<List<WriteResult>> future = batch.commit();
			List<WriteResult> results = future.get();
			log.info("Successfully committed batch write to Firestore for recording ID: {}. Results count: {}",
					recordingId, results.size());
			return recommendationsWithIds;
		} catch (ExecutionException | InterruptedException e) {
			log.error("Error committing batch write to Firestore for recording ID: {}", recordingId, e);
			if (e instanceof InterruptedException) {
				Thread.currentThread().interrupt();
			}
			return Collections.emptyList();
		} catch (Exception e) {
			log.error("Unexpected error during native Firestore batch save for recording ID: {}", recordingId, e);
			return Collections.emptyList();
		}
	}

	private void linkRecommendationsAndNotify(String userId, String recordingId, String summaryId,
			List<LearningRecommendation> savedRecommendations) {
		if (savedRecommendations == null || savedRecommendations.isEmpty()) {
			log.warn("No saved recommendations provided to link for recording ID: {}", recordingId);
			return;
		}
		List<String> newRecommendationIds = savedRecommendations.stream()
				.map(LearningRecommendation::getRecommendationId).filter(Objects::nonNull).collect(Collectors.toList());
		if (newRecommendationIds.isEmpty()) {
			log.warn("Extracted recommendation ID list is empty for recording ID: {}. Cannot link.", recordingId);
			return;
		}
		log.info("Attempting to link {} new recommendation(s) to recording ID: {}", newRecommendationIds.size(),
				recordingId);
		boolean linkSuccess = false;
		try {
			if (recordingId == null || recordingId.trim().isEmpty()) {
				log.error("Cannot link recommendations to null or empty recordingId");
				return;
			}

			Recording recording = recordingService.getRecordingById(recordingId);
			if (recording != null) {
				if (recording.getUserId() == null || recording.getUserId().trim().isEmpty()) {
					log.warn("Recording {} is missing userId. Using passed userId: {}", recordingId, userId);
					recording.setUserId(userId);
				} else if (!Objects.equals(userId, recording.getUserId())) {
					log.warn(
							"User ID mismatch! Passed userId {} does not match recording owner {} for recordingId {}. Using recording owner for notification.",
							userId, recording.getUserId(), recordingId);
					userId = recording.getUserId();
				}

				if (recording.getRecordingId() == null || recording.getRecordingId().trim().isEmpty()) {
					log.warn("Recording object has null/empty recordingId. Setting it to: {}", recordingId);
					recording.setRecordingId(recordingId);
				}

				List<String> currentIds = recording.getRecommendationIds();
				if (currentIds == null) {
					currentIds = new ArrayList<>();
				} else {
					currentIds = new ArrayList<>(currentIds);
				}
				boolean updated = false;
				for (String newId : newRecommendationIds) {
					if (!currentIds.contains(newId)) {
						currentIds.add(newId);
						updated = true;
					}
				}
				if (updated) {
					try {
						recording.setRecommendationIds(currentIds);
						recordingService.updateRecording(recording);
						log.info(
								"Successfully linked {} new recommendations. Total recommendations linked for Recording ID {}: {}",
								newRecommendationIds.size(), recordingId, currentIds.size());
						linkSuccess = true;
					} catch (IllegalArgumentException e) {
						log.error("Failed to update recording - validation error: {}", e.getMessage());
						try {
							Map<String, Object> updates = new HashMap<>();
							updates.put("recommendationIds", currentIds);
							updates.put("updatedAt", new Date());
							firestore.collection("recordings").document(recordingId).update(updates).get();
							log.info(
									"Successfully updated recommendations using direct Firestore update for recordingId {}",
									recordingId);
							linkSuccess = true;
						} catch (Exception ex) {
							log.error("Failed even with direct Firestore update: {}", ex.getMessage());
						}
					}
				} else {
					log.info(
							"No *new* recommendations to link. Recording ID {} already contains these recommendation IDs.",
							recordingId);
					linkSuccess = true;
				}
			} else {
				log.warn("Recording {} not found. Cannot link recommendations: {}", recordingId, newRecommendationIds);
				try {
					log.info("Attempting to create minimal recording object for ID: {}", recordingId);
					Recording newRecording = new Recording(recordingId, userId, "Recording " + recordingId, null);
					newRecording.setRecommendationIds(newRecommendationIds);
					recordingService.createRecording(newRecording);
					log.info("Successfully created minimal recording with recommendations for ID: {}", recordingId);
					linkSuccess = true;
				} catch (Exception e) {
					log.error("Failed to create minimal recording: {}", e.getMessage());
				}
			}
		} catch (ExecutionException | InterruptedException e) {
			log.error("Error fetching or updating recording {} to link recommendations: {}", recordingId,
					e.getMessage(), e);
			if (e instanceof InterruptedException) {
				Thread.currentThread().interrupt();
			}
		} catch (Exception e) {
			log.error("Unexpected error linking recommendations to recording {}: {}", recordingId, e.getMessage(), e);
		}

		if (linkSuccess) {
			log.info("Proceeding to send FCM notification for user: {}, recording: {}, summary: {}", userId,
					recordingId, summaryId);

			List<String> tokensToSend = userService.getFcmTokensForUser(userId);
			if (tokensToSend != null && !tokensToSend.isEmpty()) {
				MulticastMessage message = firebaseService.buildProcessingCompleteMessage(userId, recordingId,
						summaryId);
				if (message != null) {
					firebaseService.sendFcmMessage(message, tokensToSend, userId);
					log.info("FCM notification send task initiated for user {}, recording {}.", userId, recordingId);
				} else {
					log.warn(
							"FCM message build returned null (likely no tokens found for user {}). Notification not sent for recording {}.",
							userId, recordingId);
				}
			} else {
				log.warn(
						"No tokens retrieved for user {} just before sending notification for recording {}. Notification not sent.",
						userId, recordingId);
			}
		} else {
			log.error("Linking recommendations failed for recording {}. Skipping FCM notification.", recordingId);
		}
	}

	private List<LearningRecommendation> processYouTubeResults(List<SearchResult> youtubeResults, String recordingId) {
		if (youtubeResults == null || youtubeResults.isEmpty()) {
			return Collections.emptyList();
		}
		List<LearningRecommendation> recommendations = new ArrayList<>();
		Set<String> addedVideoIds = new HashSet<>();
		for (SearchResult result : youtubeResults) {
			ResourceId resourceId = result.getId();
			SearchResultSnippet snippet = result.getSnippet();
			if (resourceId == null || snippet == null) {
				log.warn("Skipping search result due to missing ID or snippet. Result: {}", result);
				continue;
			}
			String videoId = resourceId.getVideoId();
			String title = snippet.getTitle();
			if (!StringUtils.hasText(videoId) || !StringUtils.hasText(title)) {
				log.warn(
						"Skipping search result due to missing videoId or title even though ID/Snippet objects exist. VideoId: {}, Title: {}",
						videoId, title);
				continue;
			}
			if (!addedVideoIds.add(videoId)) {
				log.debug("Skipping duplicate video recommendation. VideoId: {}", videoId);
				continue;
			}
			String description = snippet.getDescription();
			String channelTitle = snippet.getChannelTitle();

			String thumbnailUrl = "https://i.ytimg.com/vi/" + videoId + "/maxresdefault.jpg";

			String fallbackThumbnailUrl = "https://i.ytimg.com/vi/" + videoId + "/hqdefault.jpg";

			Map<String, String> thumbnailOptions = new HashMap<>();
			thumbnailOptions.put("maxres", thumbnailUrl);
			thumbnailOptions.put("hq", fallbackThumbnailUrl);
			thumbnailOptions.put("mq", "https://i.ytimg.com/vi/" + videoId + "/mqdefault.jpg");
			thumbnailOptions.put("sd", "https://i.ytimg.com/vi/" + videoId + "/sddefault.jpg");
			thumbnailOptions.put("default", "https://i.ytimg.com/vi/" + videoId + "/default.jpg");

			log.debug("Using max resolution thumbnail URL for videoId {}: {} (with fallbacks)", videoId, thumbnailUrl);

			int relevanceScore = 0;

			relevanceScore += Math.min(title.length() / 5, 10);

			if (description != null) {
				relevanceScore += Math.min(description.length() / 20, 15);
			}

			boolean isEducational = false;

			if (channelTitle != null) {
				String lowerChannelTitle = channelTitle.toLowerCase();
				for (String eduTerm : EDUCATIONAL_DOMAINS) {
					if (lowerChannelTitle.contains(eduTerm)) {
						isEducational = true;
						relevanceScore += 20;
						break;
					}
				}
			}

			if (!isEducational && title != null) {
				String lowerTitle = title.toLowerCase();
				for (String eduTerm : EDUCATIONAL_DOMAINS) {
					if (lowerTitle.contains(eduTerm)) {
						isEducational = true;
						relevanceScore += 15;
						break;
					}
				}
			}

			if (title != null) {
				String lowerTitle = title.toLowerCase();
				if (lowerTitle.contains("tutorial") || lowerTitle.contains("course") || lowerTitle.contains("lecture")
						|| lowerTitle.contains("learn") || lowerTitle.contains("lesson")) {
					relevanceScore += 25;
					isEducational = true;
				}
			}

			// Heuristic language checks removed in favor of strict LanguageDetector
			// filtering upstream
			relevanceScore += 10;

			LearningRecommendation recommendation = new LearningRecommendation(videoId, title, description,
					thumbnailUrl, recordingId, relevanceScore, isEducational, channelTitle);

			recommendation.setFallbackThumbnailUrl(fallbackThumbnailUrl);

			recommendations.add(recommendation);
		}

		recommendations.sort((r1, r2) -> {
			Integer score1 = r1.getRelevanceScore() != null ? r1.getRelevanceScore() : 0;
			Integer score2 = r2.getRelevanceScore() != null ? r2.getRelevanceScore() : 0;
			return score2.compareTo(score1);
		});

		return recommendations;
	}

	public List<LearningRecommendation> getRecommendationsByRecordingId(String recordingId) {
		log.info("Attempting to retrieve recommendations for recording ID: {} using native client", recordingId);
		List<LearningRecommendation> recommendations = new ArrayList<>();
		try {
			ApiFuture<QuerySnapshot> future = firestore
					.collection(Objects.requireNonNull(recommendationsCollectionName))
					.whereEqualTo("recordingId", recordingId).get();
			QuerySnapshot querySnapshot = future.get();
			List<QueryDocumentSnapshot> documents = querySnapshot.getDocuments();
			if (documents.isEmpty()) {
				log.info("No recommendations found in Firestore for recording ID: {}", recordingId);
				return Collections.emptyList();
			}
			log.info("Found {} potential recommendation document(s) for recording ID: {}", documents.size(),
					recordingId);
			for (QueryDocumentSnapshot document : documents) {
				try {
					LearningRecommendation rec = LearningRecommendation.fromMap(document.getData());
					if (rec != null) {
						rec.setRecommendationId(document.getId());
						recommendations.add(rec);
						log.debug("Successfully mapped document {} to recommendation: {}", document.getId(), rec);
					} else {
						log.warn("Failed to map Firestore document {} to LearningRecommendation object.",
								document.getId());
					}
				} catch (Exception e) {
					log.error("Error mapping Firestore document {} to LearningRecommendation for recording ID: {}",
							document.getId(), recordingId, e);
				}
			}
			log.info("Successfully retrieved and mapped {} recommendations for recording ID: {}",
					recommendations.size(), recordingId);
			return recommendations;
		} catch (ExecutionException | InterruptedException e) {
			log.error("Error retrieving recommendations from Firestore for recording ID: {}", recordingId, e);
			if (e instanceof InterruptedException) {
				Thread.currentThread().interrupt();
			}
			return Collections.emptyList();
		} catch (Exception e) {
			log.error("Unexpected error during native Firestore retrieval for recording ID: {}", recordingId, e);
			return Collections.emptyList();
		}
	}

	public void deleteRecommendationsByRecordingId(String recordingId) {
		if (!StringUtils.hasText(recordingId)) {
			log.warn("Attempted to delete recommendations with null or empty recordingId.");
			return;
		}
		log.warn("Attempting to delete ALL recommendations for recording ID: {}", recordingId);
		CollectionReference recommendationsRef = firestore
				.collection(Objects.requireNonNull(recommendationsCollectionName));
		Query query = recommendationsRef.whereEqualTo("recordingId", recordingId);
		ApiFuture<QuerySnapshot> future = query.get();
		int deletedCount = 0;
		try {
			QuerySnapshot snapshot = future.get();
			List<QueryDocumentSnapshot> documents = snapshot.getDocuments();
			if (documents.isEmpty()) {
				log.info("No recommendations found for recording ID {} to delete.", recordingId);
				return;
			}
			WriteBatch batch = firestore.batch();
			for (QueryDocumentSnapshot document : documents) {
				log.debug("Adding recommendation {} to delete batch for recordingId {}.", document.getId(),
						recordingId);
				batch.delete(document.getReference());
				deletedCount++;
			}
			ApiFuture<List<WriteResult>> batchFuture = batch.commit();
			batchFuture.get();
			log.info("Successfully deleted {} recommendations for recording ID: {}", deletedCount, recordingId);
		} catch (ExecutionException | InterruptedException e) {
			log.error("Error deleting recommendations for recording ID {}: {}", recordingId, e.getMessage(), e);
			if (e instanceof InterruptedException) {
				Thread.currentThread().interrupt();
			}
		} catch (Exception e) {
			log.error("Unexpected error deleting recommendations for recording ID {}: {}", recordingId, e.getMessage(),
					e);
		}
	}

	@SuppressWarnings("null")
	public boolean deleteRecommendation(String recommendationId) {
		if (!StringUtils.hasText(recommendationId)) {
			log.warn("Attempted to delete recommendation with null or empty ID.");
			return false;
		}
		log.info("Attempting to delete LearningRecommendation with ID: {}", recommendationId);
		try {
			DocumentReference docRef = firestore.collection(Objects.requireNonNull(recommendationsCollectionName))
					.document(recommendationId);
			ApiFuture<WriteResult> future = docRef.delete();
			future.get();
			log.info("Successfully deleted LearningRecommendation with ID: {}", recommendationId);
			return true;
		} catch (ExecutionException e) {
			log.error("ExecutionException while deleting recommendation {}: {}", recommendationId, e.getMessage(), e);
			return false;
		} catch (InterruptedException e) {
			log.error("InterruptedException while deleting recommendation {}: {}", recommendationId, e.getMessage(), e);
			Thread.currentThread().interrupt();
			return false;
		} catch (Exception e) {
			log.error("Unexpected error deleting recommendation {}: {}", recommendationId, e.getMessage(), e);
			return false;
		}
	}

	private List<String> enhanceKeywordsWithEducationalContext(List<String> originalKeywords) {
		// With the new prompt logic, 'originalKeywords' are actually full, intent-based
		// search queries.
		// We return them as-is.
		if (originalKeywords == null) {
			return Collections.emptyList();
		}
		return originalKeywords;
	}

	private List<SearchResult> filterAndRankResults(List<SearchResult> results, List<String> keywords) {
		if (results == null || results.isEmpty()) {
			return Collections.emptyList();
		}

		Map<SearchResult, Integer> resultScores = new HashMap<>();

		for (SearchResult result : results) {
			if (result.getSnippet() == null)
				continue;

			int score = 0;
			String title = result.getSnippet().getTitle() != null ? result.getSnippet().getTitle().toLowerCase() : "";
			String description = result.getSnippet().getDescription() != null
					? result.getSnippet().getDescription().toLowerCase()
					: "";
			String channelTitle = result.getSnippet().getChannelTitle() != null
					? result.getSnippet().getChannelTitle().toLowerCase()
					: "";

			for (String keyword : keywords) {
				String lowerKeyword = keyword.toLowerCase();
				if (title.contains(lowerKeyword)) {
					score += 10;
				}
				if (description.contains(lowerKeyword)) {
					score += 5;
				}
			}

			for (String eduDomain : EDUCATIONAL_DOMAINS) {
				if (channelTitle.contains(eduDomain)) {
					score += 15;
					break;
				}
			}

			for (String eduDomain : EDUCATIONAL_DOMAINS) {
				if (title.contains(eduDomain)) {
					score += 8;
					break;
				}
			}

			if (title.length() < 15) {
				score -= 5;
			}
			if (description.length() < 30) {
				score -= 3;
			}

			resultScores.put(result, score);
		}

		return resultScores.entrySet().stream().sorted(Map.Entry.<SearchResult, Integer>comparingByValue().reversed())
				.map(Map.Entry::getKey).collect(Collectors.toList());
	}

	private List<SearchResult> filterByLanguage(List<SearchResult> results) {
		if (results == null || results.isEmpty()) {
			return Collections.emptyList();
		}

		if (languageDetector == null) {
			log.warn("Language Detector not initialized. Skipping language filter.");
			return results;
		}

		return results.stream().filter(result -> {
			if (result.getSnippet() == null)
				return false;

			String title = result.getSnippet().getTitle() != null ? result.getSnippet().getTitle() : "";
			String description = result.getSnippet().getDescription() != null
					? result.getSnippet().getDescription()
					: "";

			String textToAnalyze = (title + " " + description).trim();

			if (textToAnalyze.isEmpty()) {
				return false;
			}

			try {
				TextObject textObject = textObjectFactory.forText(textToAnalyze);
				Optional<LdLocale> language = languageDetector.detect(textObject);

				if (language.isPresent()) {
					if (language.get().getLanguage().equals("en")) {
						return true;
					} else {
						log.debug("Filtered out non-English video: {} (Detected: {})", title,
								language.get().getLanguage());
						return false;
					}
				} else {
					log.debug("Filtered out video with undetected language: {}", title);
					return false;
				}
			} catch (Exception e) {
				log.warn("Language detection error for video {}: {}", title, e.getMessage());
				return false;
			}
		}).collect(Collectors.toList());
	}
}
