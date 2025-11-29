package edu.cit.audioscholar.dto.analytics;

import java.util.Map;

/**
 * DTO for activity statistics.
 */
public class ActivityStatsDto {
	private Map<String, Long> newUsersLast30Days;
	private Map<String, Long> newRecordingsLast30Days;

	public ActivityStatsDto() {
	}

	public ActivityStatsDto(Map<String, Long> newUsersLast30Days, Map<String, Long> newRecordingsLast30Days) {
		this.newUsersLast30Days = newUsersLast30Days;
		this.newRecordingsLast30Days = newRecordingsLast30Days;
	}

	public Map<String, Long> getNewUsersLast30Days() {
		return newUsersLast30Days;
	}

	public void setNewUsersLast30Days(Map<String, Long> newUsersLast30Days) {
		this.newUsersLast30Days = newUsersLast30Days;
	}

	public Map<String, Long> getNewRecordingsLast30Days() {
		return newRecordingsLast30Days;
	}

	public void setNewRecordingsLast30Days(Map<String, Long> newRecordingsLast30Days) {
		this.newRecordingsLast30Days = newRecordingsLast30Days;
	}
}
