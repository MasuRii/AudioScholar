package edu.cit.audioscholar.dto;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import edu.cit.audioscholar.model.UserNote;

public class UserNoteDto {

	private String noteId;
	private String userId;
	private String recordingId;
	private String content;
	private List<String> tags;
	private Date createdAt;
	private Date updatedAt;

	private UserNoteDto() {
	}

	public String getNoteId() {
		return noteId;
	}

	public String getUserId() {
		return userId;
	}

	public String getRecordingId() {
		return recordingId;
	}

	public String getContent() {
		return content;
	}

	public List<String> getTags() {
		return (tags != null) ? Collections.unmodifiableList(tags) : null;
	}

	public Date getCreatedAt() {
		return (createdAt != null) ? (Date) createdAt.clone() : null;
	}

	public Date getUpdatedAt() {
		return (updatedAt != null) ? (Date) updatedAt.clone() : null;
	}

	public static UserNoteDto fromModel(UserNote userNote) {
		if (userNote == null) {
			return null;
		}

		UserNoteDto dto = new UserNoteDto();
		dto.noteId = userNote.getNoteId();
		dto.userId = userNote.getUserId();
		dto.recordingId = userNote.getRecordingId();
		dto.content = userNote.getContent();
		dto.tags = userNote.getTags();
		dto.createdAt = userNote.getCreatedAt();
		dto.updatedAt = userNote.getUpdatedAt();

		return dto;
	}
}
