package edu.cit.audioscholar.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;

import edu.cit.audioscholar.dto.analytics.ActivityStatsDto;
import edu.cit.audioscholar.dto.analytics.AnalyticsOverviewDto;
import edu.cit.audioscholar.dto.analytics.ContentEngagementDto;
import edu.cit.audioscholar.dto.analytics.UserDistributionDto;

/**
 * Service for retrieving analytics data for the admin dashboard.
 */
@Service
public class AnalyticsService {

	private static final Logger log = LoggerFactory.getLogger(AnalyticsService.class);
	private static final String USERS_COLLECTION = "users";
	private static final String RECORDINGS_COLLECTION = "recordings";
	private static final String ROLE_USER = "ROLE_USER";

	private final FirebaseService firebaseService;

	public AnalyticsService(FirebaseService firebaseService) {
		this.firebaseService = firebaseService;
	}

	/**
	 * Retrieves overview statistics including total users, recordings, storage
	 * usage, and duration.
	 *
	 * @return AnalyticsOverviewDto containing the aggregated stats.
	 */
	public AnalyticsOverviewDto getOverviewStats() {
		log.info("Fetching overview stats...");
		long totalUsers = firebaseService.getCollectionCount(USERS_COLLECTION);
		long totalRecordings = firebaseService.getCollectionCount(RECORDINGS_COLLECTION);

		List<Map<String, Object>> recordings = firebaseService.getProjectedData(RECORDINGS_COLLECTION, "fileSize",
				"duration", "recordingId");

		long totalStorage = 0;
		long totalDuration = 0;

		for (Map<String, Object> recData : recordings) {
			// Using Map directly to avoid overhead of full object mapping
			Object fileSizeObj = recData.get("fileSize");
			if (fileSizeObj instanceof Number) {
				totalStorage += ((Number) fileSizeObj).longValue();
			}

			Object durationObj = recData.get("duration");
			if (durationObj instanceof String) {
				totalDuration += parseDuration((String) durationObj);
			}
		}

		return new AnalyticsOverviewDto(totalUsers, totalRecordings, totalStorage, totalDuration);
	}

	private long parseDuration(String durationStr) {
		if (durationStr == null || durationStr.isBlank())
			return 0;
		try {
			// Expected formats: "HH:MM:SS", "MM:SS", or simple seconds string
			String[] parts = durationStr.split(":");
			long seconds = 0;
			if (parts.length == 3) {
				seconds += Long.parseLong(parts[0]) * 3600;
				seconds += Long.parseLong(parts[1]) * 60;
				seconds += Long.parseLong(parts[2]);
			} else if (parts.length == 2) {
				seconds += Long.parseLong(parts[0]) * 60;
				seconds += Long.parseLong(parts[1]);
			} else {
				// Try parsing as raw seconds
				double val = Double.parseDouble(durationStr);
				seconds = (long) val;
			}
			return seconds;
		} catch (Exception e) {
			// ignore parsing errors, treat as 0
			return 0;
		}
	}

	/**
	 * Retrieves activity statistics for the last 30 days.
	 *
	 * @return ActivityStatsDto containing new users and recordings count per day.
	 */
	public ActivityStatsDto getActivityStats() {
		log.info("Fetching activity stats...");
		Date thirtyDaysAgoDate = Date
				.from(LocalDate.now().minusDays(30).atStartOfDay(ZoneId.systemDefault()).toInstant());

		ApiFuture<QuerySnapshot> usersFuture = firebaseService.getDocumentsSince(USERS_COLLECTION, "createdAt",
				thirtyDaysAgoDate);
		ApiFuture<QuerySnapshot> recordingsFuture = firebaseService.getDocumentsSince(RECORDINGS_COLLECTION,
				"createdAt", thirtyDaysAgoDate);

		Map<String, Long> newUsers = new HashMap<>();
		Map<String, Long> newRecordings = new HashMap<>();

		try {
			List<QueryDocumentSnapshot> users = usersFuture.get().getDocuments();
			List<QueryDocumentSnapshot> recordings = recordingsFuture.get().getDocuments();

			for (QueryDocumentSnapshot userDoc : users) {
				Date createdAt = userDoc.getDate("createdAt");
				if (createdAt != null) {
					LocalDate createdDate = createdAt.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
					newUsers.merge(createdDate.toString(), 1L, Long::sum);
				}
			}

			for (QueryDocumentSnapshot recDoc : recordings) {
				Date createdAt = recDoc.getDate("createdAt");
				if (createdAt != null) {
					LocalDate createdDate = createdAt.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
					newRecordings.merge(createdDate.toString(), 1L, Long::sum);
				}
			}

		} catch (InterruptedException | ExecutionException e) {
			Thread.currentThread().interrupt();
			log.error("Error fetching activity stats", e);
		}

		return new ActivityStatsDto(newUsers, newRecordings);
	}

	/**
	 * Retrieves user distribution statistics by provider and role.
	 *
	 * @return UserDistributionDto containing user counts grouped by provider and
	 *         role.
	 */
	public UserDistributionDto getUserDistribution() {
		log.info("Fetching user distribution stats...");
		List<Map<String, Object>> users = firebaseService.getProjectedData(USERS_COLLECTION, "provider", "roles");

		Map<String, Long> byProvider = new HashMap<>();
		Map<String, Long> byRole = new HashMap<>();

		for (Map<String, Object> userMap : users) {
			// Directly access map
			Object providerObj = userMap.get("provider");
			String provider = (providerObj != null) ? providerObj.toString() : "unknown";
			byProvider.merge(provider, 1L, Long::sum);

			Object rolesObj = userMap.get("roles");
			if (rolesObj instanceof List<?>) {
				List<?> roles = (List<?>) rolesObj;
				if (!roles.isEmpty()) {
					for (Object role : roles) {
						if (role != null) {
							byRole.merge(role.toString(), 1L, Long::sum);
						}
					}
				} else {
					byRole.merge(ROLE_USER, 1L, Long::sum);
				}
			} else {
				byRole.merge(ROLE_USER, 1L, Long::sum);
			}
		}
		return new UserDistributionDto(byProvider, byRole);
	}

	/**
	 * Retrieves the top 10 most engaging content items based on favorite count.
	 *
	 * @return List of ContentEngagementDto representing top recordings.
	 */
	public List<ContentEngagementDto> getContentEngagement() {
		log.info("Fetching content engagement stats...");
		List<Map<String, Object>> topRecordings = firebaseService.getTopRecordings(10);

		List<ContentEngagementDto> result = new ArrayList<>();
		for (Map<String, Object> r : topRecordings) {
			String recordingId = (String) r.get("recordingId");
			String title = (String) r.get("title");
			Object favCountObj = r.get("favoriteCount");
			int favoriteCount = 0;
			if (favCountObj instanceof Number) {
				favoriteCount = ((Number) favCountObj).intValue();
			}

			result.add(new ContentEngagementDto(recordingId, title, favoriteCount));
		}
		return result;
	}
}
