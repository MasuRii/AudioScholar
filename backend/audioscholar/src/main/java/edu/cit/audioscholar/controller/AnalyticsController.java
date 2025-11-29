package edu.cit.audioscholar.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.cit.audioscholar.dto.analytics.ActivityStatsDto;
import edu.cit.audioscholar.dto.analytics.AnalyticsOverviewDto;
import edu.cit.audioscholar.dto.analytics.ContentEngagementDto;
import edu.cit.audioscholar.dto.analytics.UserDistributionDto;
import edu.cit.audioscholar.service.AnalyticsService;

/**
 * Controller for admin analytics. Provides endpoints for retrieving system-wide
 * statistics and usage data.
 */
@RestController
@RequestMapping("/api/admin/analytics")
@PreAuthorize("hasRole('ADMIN')")
public class AnalyticsController {

	private final AnalyticsService analyticsService;

	public AnalyticsController(AnalyticsService analyticsService) {
		this.analyticsService = analyticsService;
	}

	/**
	 * Retrieves overview statistics.
	 *
	 * @return AnalyticsOverviewDto containing system totals.
	 */
	@GetMapping("/overview")
	public ResponseEntity<AnalyticsOverviewDto> getOverview() {
		return ResponseEntity.ok(analyticsService.getOverviewStats());
	}

	/**
	 * Retrieves activity statistics over the last 30 days.
	 *
	 * @return ActivityStatsDto containing daily activity counts.
	 */
	@GetMapping("/activity")
	public ResponseEntity<ActivityStatsDto> getActivity() {
		return ResponseEntity.ok(analyticsService.getActivityStats());
	}

	/**
	 * Retrieves user distribution statistics.
	 *
	 * @return UserDistributionDto containing user counts by provider and role.
	 */
	@GetMapping("/users/distribution")
	public ResponseEntity<UserDistributionDto> getUserDistribution() {
		return ResponseEntity.ok(analyticsService.getUserDistribution());
	}

	/**
	 * Retrieves content engagement statistics.
	 *
	 * @return List of ContentEngagementDto containing top engaging content.
	 */
	@GetMapping("/content/engagement")
	public ResponseEntity<List<ContentEngagementDto>> getContentEngagement() {
		return ResponseEntity.ok(analyticsService.getContentEngagement());
	}
}
