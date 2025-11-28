package edu.cit.audioscholar.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import edu.cit.audioscholar.dto.CreateUserNoteRequest;
import edu.cit.audioscholar.dto.UpdateUserNoteRequest;
import edu.cit.audioscholar.model.Recording;
import edu.cit.audioscholar.model.UserNote;

@ExtendWith(MockitoExtension.class)
public class UserNoteServiceTest {

	@Mock
	private FirebaseService firebaseService;

	@Mock
	private RecordingService recordingService;

	private UserNoteService userNoteService;

	private static final String USER_ID = "user-123";
	private static final String OTHER_USER_ID = "user-456";
	private static final String RECORDING_ID = "recording-123";
	private static final String NOTE_ID = "note-789";

	@BeforeEach
	void setUp() {
		userNoteService = new UserNoteService(firebaseService, recordingService);
	}

	@Test
	void testCreateNote_Success() throws ExecutionException, InterruptedException {
		CreateUserNoteRequest request = new CreateUserNoteRequest(RECORDING_ID, "My note content",
				Arrays.asList("tag1", "tag2"));

		Recording recording = new Recording();
		recording.setRecordingId(RECORDING_ID);
		recording.setUserId(USER_ID);

		when(recordingService.getRecordingById(RECORDING_ID)).thenReturn(recording);
		when(firebaseService.saveData(eq("user_notes"), anyString(), anyMap())).thenReturn("timestamp");

		UserNote createdNote = userNoteService.createNote(USER_ID, request);

		assertNotNull(createdNote);
		assertNotNull(createdNote.getNoteId());
		assertEquals(USER_ID, createdNote.getUserId());
		assertEquals(RECORDING_ID, createdNote.getRecordingId());
		assertEquals("My note content", createdNote.getContent());
		assertEquals(2, createdNote.getTags().size());
		verify(firebaseService).saveData(eq("user_notes"), anyString(), anyMap());
	}

	@Test
	void testCreateNote_RecordingNotFound() throws ExecutionException, InterruptedException {
		CreateUserNoteRequest request = new CreateUserNoteRequest("invalid-rec-id", "Content", null);

		when(recordingService.getRecordingById("invalid-rec-id")).thenReturn(null);

		assertThrows(IllegalArgumentException.class, () -> {
			userNoteService.createNote(USER_ID, request);
		});

		verify(firebaseService, never()).saveData(anyString(), anyString(), anyMap());
	}

	@Test
	void testCreateNote_AccessDenied_NotOwnerOfRecording() throws ExecutionException, InterruptedException {
		CreateUserNoteRequest request = new CreateUserNoteRequest(RECORDING_ID, "Content", null);

		Recording recording = new Recording();
		recording.setRecordingId(RECORDING_ID);
		recording.setUserId(OTHER_USER_ID); // Belongs to someone else

		when(recordingService.getRecordingById(RECORDING_ID)).thenReturn(recording);

		assertThrows(AccessDeniedException.class, () -> {
			userNoteService.createNote(USER_ID, request);
		});

		verify(firebaseService, never()).saveData(anyString(), anyString(), anyMap());
	}

	@Test
	void testGetNotesByRecordingId_Success() {
		UserNote note1 = new UserNote("id1", USER_ID, RECORDING_ID, "Note 1", null);
		UserNote note2 = new UserNote("id2", OTHER_USER_ID, RECORDING_ID, "Note 2", null); // Should be filtered out

		Map<String, Object> map1 = note1.toMap();
		Map<String, Object> map2 = note2.toMap();

		// Mock firebase returning notes for this recording (potentially mixed users if
		// query was broad,
		// though service impl assumes query returns list and filters in stream)
		when(firebaseService.queryCollection("user_notes", "recordingId", RECORDING_ID))
				.thenReturn(Arrays.asList(map1, map2));

		List<UserNote> results = userNoteService.getNotesByRecordingId(USER_ID, RECORDING_ID);

		assertEquals(1, results.size());
		assertEquals("id1", results.get(0).getNoteId());
	}

	@Test
	void testGetNoteById_Success() {
		UserNote note = new UserNote(NOTE_ID, USER_ID, RECORDING_ID, "Content", null);
		Map<String, Object> noteMap = note.toMap();

		when(firebaseService.getData("user_notes", NOTE_ID)).thenReturn(noteMap);

		UserNote result = userNoteService.getNoteById(USER_ID, NOTE_ID);

		assertNotNull(result);
		assertEquals(NOTE_ID, result.getNoteId());
		assertEquals(USER_ID, result.getUserId());
	}

	@Test
	void testGetNoteById_NotFound() {
		when(firebaseService.getData("user_notes", "missing-id")).thenReturn(null);

		assertThrows(IllegalArgumentException.class, () -> {
			userNoteService.getNoteById(USER_ID, "missing-id");
		});
	}

	@Test
	void testGetNoteById_AccessDenied() {
		UserNote note = new UserNote(NOTE_ID, OTHER_USER_ID, RECORDING_ID, "Content", null);
		Map<String, Object> noteMap = note.toMap();

		when(firebaseService.getData("user_notes", NOTE_ID)).thenReturn(noteMap);

		assertThrows(AccessDeniedException.class, () -> {
			userNoteService.getNoteById(USER_ID, NOTE_ID);
		});
	}

	@Test
	void testUpdateNote_Success() {
		UserNote existingNote = new UserNote(NOTE_ID, USER_ID, RECORDING_ID, "Old Content", Arrays.asList("old"));
		Map<String, Object> existingMap = existingNote.toMap();

		when(firebaseService.getData("user_notes", NOTE_ID)).thenReturn(existingMap);
		when(firebaseService.updateData(eq("user_notes"), eq(NOTE_ID), anyMap())).thenReturn("timestamp");

		UpdateUserNoteRequest request = new UpdateUserNoteRequest("New Content", Arrays.asList("new"));

		UserNote updatedNote = userNoteService.updateNote(USER_ID, NOTE_ID, request);

		assertEquals("New Content", updatedNote.getContent());
		assertEquals("new", updatedNote.getTags().get(0));
		verify(firebaseService).updateData(eq("user_notes"), eq(NOTE_ID), anyMap());
	}

	@Test
	void testUpdateNote_AccessDenied() {
		UserNote existingNote = new UserNote(NOTE_ID, OTHER_USER_ID, RECORDING_ID, "Old Content", null);
		Map<String, Object> existingMap = existingNote.toMap();

		when(firebaseService.getData("user_notes", NOTE_ID)).thenReturn(existingMap);

		UpdateUserNoteRequest request = new UpdateUserNoteRequest("New Content", null);

		assertThrows(AccessDeniedException.class, () -> {
			userNoteService.updateNote(USER_ID, NOTE_ID, request);
		});

		verify(firebaseService, never()).updateData(anyString(), anyString(), anyMap());
	}

	@Test
	void testDeleteNote_Success() {
		UserNote existingNote = new UserNote(NOTE_ID, USER_ID, RECORDING_ID, "Content", null);
		Map<String, Object> existingMap = existingNote.toMap();

		when(firebaseService.getData("user_notes", NOTE_ID)).thenReturn(existingMap);
		when(firebaseService.deleteData("user_notes", NOTE_ID)).thenReturn("timestamp");

		userNoteService.deleteNote(USER_ID, NOTE_ID);

		verify(firebaseService).deleteData("user_notes", NOTE_ID);
	}

	@Test
	void testDeleteNote_AccessDenied() {
		UserNote existingNote = new UserNote(NOTE_ID, OTHER_USER_ID, RECORDING_ID, "Content", null);
		Map<String, Object> existingMap = existingNote.toMap();

		when(firebaseService.getData("user_notes", NOTE_ID)).thenReturn(existingMap);

		assertThrows(AccessDeniedException.class, () -> {
			userNoteService.deleteNote(USER_ID, NOTE_ID);
		});

		verify(firebaseService, never()).deleteData(anyString(), anyString());
	}
}
