package edu.cit.audioscholar.dto;

import java.util.List;

public class UpdateUserNoteRequest {

	private String content;
	private List<String> tags;

	public UpdateUserNoteRequest() {
	}

	public UpdateUserNoteRequest(String content, List<String> tags) {
		this.content = content;
		this.tags = tags;
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
