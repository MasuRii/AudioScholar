package edu.cit.audioscholar.service;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import edu.cit.audioscholar.dto.CreateUserNoteRequest;
import edu.cit.audioscholar.dto.UpdateUserNoteRequest;
import edu.cit.audioscholar.model.Recording;
import edu.cit.audioscholar.model.UserNote;

@Service
public class UserNoteService {

	private static final Logger log = LoggerFactory.getLogger(UserNoteService.class);
	private static final String USER_NOTES_COLLECTION = "user_notes";

	private final FirebaseService firebaseService;
	private final RecordingService recordingService;

	public UserNoteService(FirebaseService firebaseService, RecordingService recordingService) {
		this.firebaseService = firebaseService;
		this.recordingService = recordingService;
	}

	public UserNote createNote(String userId, CreateUserNoteRequest request)
			throws ExecutionException, InterruptedException {
		if (!StringUtils.hasText(userId)) {
			throw new IllegalArgumentException("User ID cannot be null or empty.");
		}
		if (request == null || !StringUtils.hasText(request.getRecordingId())) {
			throw new IllegalArgumentException("Request or Recording ID cannot be null or empty.");
		}

		// Validate recording existence and ownership
		Recording recording = recordingService.getRecordingById(request.getRecordingId());
		if (recording == null) {
			throw new IllegalArgumentException("Recording not found with ID: " + request.getRecordingId());
		}
		if (!recording.getUserId().equals(userId)) {
			log.warn("User {} attempted to create note for recording {} owned by {}", userId, request.getRecordingId(),
					recording.getUserId());
			throw new AccessDeniedException("You do not have permission to add notes to this recording.");
		}

		String noteId = UUID.randomUUID().toString();
		UserNote note = new UserNote();
		note.setNoteId(noteId);
		note.setUserId(userId);
		note.setRecordingId(request.getRecordingId());
		note.setContent(request.getContent());
		note.setTags(request.getTags());
		note.setCreatedAt(new Date());
		note.setUpdatedAt(new Date());

		log.info("Saving new UserNote (ID: {}) for user {} and recording {}", noteId, userId, request.getRecordingId());
		firebaseService.saveData(USER_NOTES_COLLECTION, noteId, note.toMap());

		return note;
	}

	public List<UserNote> getNotesByRecordingId(String userId, String recordingId) {
		if (!StringUtils.hasText(userId) || !StringUtils.hasText(recordingId)) {
			throw new IllegalArgumentException("User ID and Recording ID must be provided.");
		}

		log.debug("Fetching notes for recording {} and user {}", recordingId, userId);

		// Note: FirebaseService.queryCollection supports only simple equality queries
		// on one field.
		// Assuming notes for a recording generally belong to the recording owner.
		// We filter by userId in memory to ensure security compliance.
		List<Map<String, Object>> results = firebaseService.queryCollection(USER_NOTES_COLLECTION, "recordingId",
				recordingId);

		return results.stream().map(data -> {
			String id = (String) data.get("noteId");
			if (id == null) {
				id = (String) data.get("id");
			}
			return UserNote.fromMap(id, data);
		}).filter(note -> note != null && userId.equals(note.getUserId())).collect(Collectors.toList());
	}

	public UserNote getNoteById(String userId, String noteId) {
		if (!StringUtils.hasText(userId) || !StringUtils.hasText(noteId)) {
			throw new IllegalArgumentException("User ID and Note ID must be provided.");
		}

		log.debug("Fetching note by ID: {}", noteId);
		Map<String, Object> data = firebaseService.getData(USER_NOTES_COLLECTION, noteId);

		if (data == null) {
			throw new IllegalArgumentException("Note not found with ID: " + noteId);
		}

		UserNote note = UserNote.fromMap(noteId, data);
		if (note == null) {
			throw new IllegalStateException("Failed to map note data for ID: " + noteId);
		}

		if (!userId.equals(note.getUserId())) {
			log.warn("User {} attempted to access note {} owned by {}", userId, noteId, note.getUserId());
			throw new AccessDeniedException("You do not have permission to access this note.");
		}

		return note;
	}

	public UserNote updateNote(String userId, String noteId, UpdateUserNoteRequest request) {
		UserNote note = getNoteById(userId, noteId); // Validates existence and ownership

		boolean updated = false;
		if (request.getContent() != null) {
			note.setContent(request.getContent());
			updated = true;
		}
		if (request.getTags() != null) {
			note.setTags(request.getTags());
			updated = true;
		}

		if (updated) {
			note.setUpdatedAt(new Date());
			log.info("Updating UserNote (ID: {}) for user {}", noteId, userId);
			firebaseService.updateData(USER_NOTES_COLLECTION, noteId, note.toMap());
		}

		return note;
	}

	public void deleteNote(String userId, String noteId) {
		// Validates existence and ownership implicitly via getNoteById, but we can do
		// it explicitly/efficiently if needed.
		// Reusing getNoteById to ensure consistent security checks.
		try {
			UserNote note = getNoteById(userId, noteId);
			log.info("Deleting UserNote (ID: {}) for user {}", noteId, userId);
			firebaseService.deleteData(USER_NOTES_COLLECTION, note.getNoteId());
		} catch (IllegalArgumentException e) {
			log.warn("Attempted to delete non-existent note or note error: {}", e.getMessage());
			// If note doesn't exist, we might consider it 'deleted' or throw.
			// Requirement says "Fetch note -> Validate ownership -> Delete".
			// getNoteById throws IllegalArgumentException if not found.
			throw e;
		}
	}
}
