package edu.cit.audioscholar.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class UserNote {

	private String noteId;
	private String userId;
	private String recordingId;
	private String content;
	private List<String> tags;
	private Date createdAt;
	private Date updatedAt;

	public UserNote() {
		this.createdAt = new Date();
		this.updatedAt = new Date();
		this.tags = new ArrayList<>();
	}

	public UserNote(String noteId, String userId, String recordingId, String content, List<String> tags) {
		this();
		this.noteId = noteId;
		this.userId = userId;
		this.recordingId = recordingId;
		this.content = content;
		if (tags != null) {
			this.tags = new ArrayList<>(tags);
		}
	}

	public String getNoteId() {
		return noteId;
	}

	public void setNoteId(String noteId) {
		this.noteId = noteId;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
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
		this.tags = (tags != null) ? new ArrayList<>(tags) : new ArrayList<>();
	}

	public Date getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}

	public Date getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Date updatedAt) {
		this.updatedAt = updatedAt;
	}

	public Map<String, Object> toMap() {
		Map<String, Object> map = new HashMap<>();
		if (noteId != null)
			map.put("noteId", noteId);
		if (userId != null)
			map.put("userId", userId);
		if (recordingId != null)
			map.put("recordingId", recordingId);
		if (content != null)
			map.put("content", content);
		if (tags != null && !tags.isEmpty())
			map.put("tags", tags);
		if (createdAt != null)
			map.put("createdAt", createdAt);
		if (updatedAt != null)
			map.put("updatedAt", updatedAt);
		return map;
	}

	public static UserNote fromMap(Map<String, Object> map) {
		if (map == null) {
			return null;
		}
		UserNote note = new UserNote();
		note.noteId = (String) map.get("noteId");
		note.userId = (String) map.get("userId");
		note.recordingId = (String) map.get("recordingId");
		note.content = (String) map.get("content");

		Object tagsObj = map.get("tags");
		if (tagsObj instanceof List) {
			try {
				List<?> rawList = (List<?>) tagsObj;
				List<String> stringList = new ArrayList<>();
				for (Object item : rawList) {
					if (item instanceof String) {
						stringList.add((String) item);
					} else if (item != null) {
						stringList.add(item.toString());
					}
				}
				note.tags = stringList;
			} catch (Exception e) {
				System.err.println("Warning: Could not cast tags list items to String. List content: " + tagsObj);
				note.tags = new ArrayList<>();
			}
		} else {
			note.tags = new ArrayList<>();
		}

		Object createdAtObj = map.get("createdAt");
		if (createdAtObj instanceof com.google.cloud.Timestamp) {
			note.createdAt = ((com.google.cloud.Timestamp) createdAtObj).toDate();
		} else if (createdAtObj instanceof Date) {
			note.createdAt = (Date) createdAtObj;
		}

		Object updatedAtObj = map.get("updatedAt");
		if (updatedAtObj instanceof com.google.cloud.Timestamp) {
			note.updatedAt = ((com.google.cloud.Timestamp) updatedAtObj).toDate();
		} else if (updatedAtObj instanceof Date) {
			note.updatedAt = (Date) updatedAtObj;
		}

		return note;
	}

	public static UserNote fromMap(String documentId, Map<String, Object> map) {
		UserNote note = fromMap(map);
		if (note != null) {
			note.noteId = documentId;
		}
		return note;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		UserNote userNote = (UserNote) o;
		return Objects.equals(noteId, userNote.noteId) && Objects.equals(userId, userNote.userId)
				&& Objects.equals(recordingId, userNote.recordingId) && Objects.equals(content, userNote.content)
				&& Objects.equals(tags, userNote.tags) && Objects.equals(createdAt, userNote.createdAt)
				&& Objects.equals(updatedAt, userNote.updatedAt);
	}

	@Override
	public int hashCode() {
		return Objects.hash(noteId, userId, recordingId, content, tags, createdAt, updatedAt);
	}

	@Override
	public String toString() {
		return "UserNote{" + "noteId='" + noteId + '\'' + ", userId='" + userId + '\'' + ", recordingId='" + recordingId
				+ '\'' + ", content='" + content + '\'' + ", tags=" + tags + ", createdAt=" + createdAt + ", updatedAt="
				+ updatedAt + '}';
	}
}
