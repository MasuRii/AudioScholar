package edu.cit.audioscholar.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.cit.audioscholar.dto.CreateUserNoteRequest;
import edu.cit.audioscholar.dto.UpdateUserNoteRequest;
import edu.cit.audioscholar.model.UserNote;
import edu.cit.audioscholar.service.UserNoteService;

@ExtendWith(MockitoExtension.class)
class UserNoteControllerTest {

	private MockMvc mockMvc;
	private ObjectMapper objectMapper = new ObjectMapper();

	@Mock
	private UserNoteService userNoteService;

	@InjectMocks
	private UserNoteController userNoteController;

	private final String USER_ID = "testUser123";
	private final String RECORDING_ID = "rec123";
	private final String NOTE_ID = "note123";

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(userNoteController).build();
	}

	private Authentication createAuthentication(String userId) {
		Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").claim("sub", userId).build();
		return new UsernamePasswordAuthenticationToken(jwt, null, Collections.emptyList());
	}

	@Test
	void createNote_Success() throws Exception {
		CreateUserNoteRequest request = new CreateUserNoteRequest(RECORDING_ID, "My Note", List.of("tag1"));
		UserNote note = new UserNote(NOTE_ID, USER_ID, RECORDING_ID, "My Note", List.of("tag1"));

		when(userNoteService.createNote(eq(USER_ID), any(CreateUserNoteRequest.class))).thenReturn(note);

		mockMvc.perform(post("/api/notes").principal(createAuthentication(USER_ID))
				.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated()).andExpect(jsonPath("$.noteId").value(NOTE_ID))
				.andExpect(jsonPath("$.content").value("My Note"));
	}

	@Test
	void getNotesForRecording_Success() throws Exception {
		UserNote note = new UserNote(NOTE_ID, USER_ID, RECORDING_ID, "My Note", List.of("tag1"));
		when(userNoteService.getNotesByRecordingId(USER_ID, RECORDING_ID)).thenReturn(List.of(note));

		mockMvc.perform(get("/api/notes").principal(createAuthentication(USER_ID)).param("recordingId", RECORDING_ID))
				.andExpect(status().isOk()).andExpect(jsonPath("$[0].noteId").value(NOTE_ID));
	}

	@Test
	void getNote_Success() throws Exception {
		UserNote note = new UserNote(NOTE_ID, USER_ID, RECORDING_ID, "My Note", List.of("tag1"));
		when(userNoteService.getNoteById(USER_ID, NOTE_ID)).thenReturn(note);

		mockMvc.perform(get("/api/notes/{noteId}", NOTE_ID).principal(createAuthentication(USER_ID)))
				.andExpect(status().isOk()).andExpect(jsonPath("$.noteId").value(NOTE_ID));
	}

	@Test
	void updateNote_Success() throws Exception {
		UpdateUserNoteRequest request = new UpdateUserNoteRequest("Updated Content", List.of("tag2"));
		UserNote note = new UserNote(NOTE_ID, USER_ID, RECORDING_ID, "Updated Content", List.of("tag2"));

		when(userNoteService.updateNote(eq(USER_ID), eq(NOTE_ID), any(UpdateUserNoteRequest.class))).thenReturn(note);

		mockMvc.perform(patch("/api/notes/{noteId}", NOTE_ID).principal(createAuthentication(USER_ID))
				.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk()).andExpect(jsonPath("$.content").value("Updated Content"));
	}

	@Test
	void deleteNote_Success() throws Exception {
		mockMvc.perform(delete("/api/notes/{noteId}", NOTE_ID).principal(createAuthentication(USER_ID)))
				.andExpect(status().isNoContent());
	}

	@Test
	void getNote_NotFound() throws Exception {
		when(userNoteService.getNoteById(USER_ID, NOTE_ID)).thenThrow(new IllegalArgumentException("Not found"));

		mockMvc.perform(get("/api/notes/{noteId}", NOTE_ID).principal(createAuthentication(USER_ID)))
				.andExpect(status().isNotFound());
	}
}
