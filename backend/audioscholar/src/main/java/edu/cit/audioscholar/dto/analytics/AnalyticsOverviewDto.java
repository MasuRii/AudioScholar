package edu.cit.audioscholar.dto.analytics;

/**
 * DTO for analytics overview data.
 */
public class AnalyticsOverviewDto {
	private long totalUsers;
	private long totalRecordings;
	private long totalStorageBytes;
	private long totalDurationSeconds;

	public AnalyticsOverviewDto() {
	}

	public AnalyticsOverviewDto(long totalUsers, long totalRecordings, long totalStorageBytes,
			long totalDurationSeconds) {
		this.totalUsers = totalUsers;
		this.totalRecordings = totalRecordings;
		this.totalStorageBytes = totalStorageBytes;
		this.totalDurationSeconds = totalDurationSeconds;
	}

	public long getTotalUsers() {
		return totalUsers;
	}

	public void setTotalUsers(long totalUsers) {
		this.totalUsers = totalUsers;
	}

	public long getTotalRecordings() {
		return totalRecordings;
	}

	public void setTotalRecordings(long totalRecordings) {
		this.totalRecordings = totalRecordings;
	}

	public long getTotalStorageBytes() {
		return totalStorageBytes;
	}

	public void setTotalStorageBytes(long totalStorageBytes) {
		this.totalStorageBytes = totalStorageBytes;
	}

	public long getTotalDurationSeconds() {
		return totalDurationSeconds;
	}

	public void setTotalDurationSeconds(long totalDurationSeconds) {
		this.totalDurationSeconds = totalDurationSeconds;
	}
}
