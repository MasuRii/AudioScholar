package edu.cit.audioscholar.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.cit.audioscholar.dto.UpdateRecordingRequest;
import edu.cit.audioscholar.model.Recording;
import edu.cit.audioscholar.service.AudioProcessingService;
import edu.cit.audioscholar.service.FirebaseService;
import edu.cit.audioscholar.service.RecordingService;

@ExtendWith(MockitoExtension.class)
public class AudioControllerTest {

	private MockMvc mockMvc;
	private ObjectMapper objectMapper = new ObjectMapper();

	@Mock
	private RecordingService recordingService;

	@Mock
	private FirebaseService firebaseService;

	@Mock
	private AudioProcessingService audioProcessingService;

	@InjectMocks
	private AudioController audioController;

	private final String TEST_USER_ID = "testUser123";
	private final String OTHER_USER_ID = "otherUser456";
	private final String RECORDING_ID = "rec123";

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(audioController).build();
		SecurityContextHolder.getContext()
				.setAuthentication(new UsernamePasswordAuthenticationToken(TEST_USER_ID, null, null));
	}

	@Test
	void updateRecording_Success() throws Exception {
		Recording recording = new Recording();
		recording.setRecordingId(RECORDING_ID);
		recording.setUserId(TEST_USER_ID);
		recording.setTitle("Old Title");

		UpdateRecordingRequest request = new UpdateRecordingRequest("New Title", "New Description");

		when(recordingService.getRecordingById(RECORDING_ID)).thenReturn(recording);

		mockMvc.perform(patch("/api/audio/recordings/{recordingId}", RECORDING_ID)
				.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk()).andExpect(jsonPath("$.title").value("New Title"))
				.andExpect(jsonPath("$.description").value("New Description"));
	}

	@Test
	void updateRecording_Forbidden() throws Exception {
		Recording recording = new Recording();
		recording.setRecordingId(RECORDING_ID);
		recording.setUserId(OTHER_USER_ID);

		UpdateRecordingRequest request = new UpdateRecordingRequest("New Title", null);

		when(recordingService.getRecordingById(RECORDING_ID)).thenReturn(recording);

		mockMvc.perform(patch("/api/audio/recordings/{recordingId}", RECORDING_ID)
				.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isForbidden());
	}

	@Test
	void updateRecording_NotFound() throws Exception {
		UpdateRecordingRequest request = new UpdateRecordingRequest("New Title", null);

		when(recordingService.getRecordingById(RECORDING_ID)).thenReturn(null);

		mockMvc.perform(patch("/api/audio/recordings/{recordingId}", RECORDING_ID)
				.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isNotFound());
	}
}
