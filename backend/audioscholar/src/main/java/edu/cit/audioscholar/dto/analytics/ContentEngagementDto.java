package edu.cit.audioscholar.dto.analytics;

/**
 * DTO for content engagement metrics.
 */
public class ContentEngagementDto {
	private String recordingId;
	private String title;
	private int favoriteCount;

	public ContentEngagementDto() {
	}

	public ContentEngagementDto(String recordingId, String title, int favoriteCount) {
		this.recordingId = recordingId;
		this.title = title;
		this.favoriteCount = favoriteCount;
	}

	public String getRecordingId() {
		return recordingId;
	}

	public void setRecordingId(String recordingId) {
		this.recordingId = recordingId;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public int getFavoriteCount() {
		return favoriteCount;
	}

	public void setFavoriteCount(int favoriteCount) {
		this.favoriteCount = favoriteCount;
	}
}
