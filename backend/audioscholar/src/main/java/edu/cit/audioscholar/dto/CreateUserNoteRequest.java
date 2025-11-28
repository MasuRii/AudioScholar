package edu.cit.audioscholar.dto;

import java.util.List;

public class CreateUserNoteRequest {

	private String recordingId;
	private String content;
	private List<String> tags;

	public CreateUserNoteRequest() {
	}

	public CreateUserNoteRequest(String recordingId, String content, List<String> tags) {
		this.recordingId = recordingId;
		this.content = content;
		this.tags = tags;
	}

	public String getRecordingId() {
		return recordingId;
	}

	public void setRecordingId(String recordingId) {
		this.recordingId = recordingId;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public List<String> getTags() {
		return tags;
	}

	public void setTags(List<String> tags) {
		this.tags = tags;
	}
}
