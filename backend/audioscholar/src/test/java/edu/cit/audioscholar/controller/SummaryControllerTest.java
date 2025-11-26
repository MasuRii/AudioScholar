package edu.cit.audioscholar.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;

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

import edu.cit.audioscholar.dto.UpdateSummaryRequest;
import edu.cit.audioscholar.model.Recording;
import edu.cit.audioscholar.model.Summary;
import edu.cit.audioscholar.service.FirebaseService;
import edu.cit.audioscholar.service.RecordingService;
import edu.cit.audioscholar.service.SummaryService;

@ExtendWith(MockitoExtension.class)
public class SummaryControllerTest {

	private MockMvc mockMvc;
	private ObjectMapper objectMapper = new ObjectMapper();

	@Mock
	private SummaryService summaryService;

	@Mock
	private RecordingService recordingService;

	@Mock
	private FirebaseService firebaseService;

	@InjectMocks
	private SummaryController summaryController;

	private final String TEST_USER_ID = "testUser123";
	private final String OTHER_USER_ID = "otherUser456";
	private final String SUMMARY_ID = "sum123";
	private final String RECORDING_ID = "rec123";

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(summaryController).build();
		// SecurityContextHolder.getContext().setAuthentication(auth); // The controller
		// gets it as a parameter
	}

	private Authentication createAuthentication(String userId) {
		Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").claim("sub", userId).build();
		return new UsernamePasswordAuthenticationToken(jwt, null, Collections.emptyList());
	}

	@Test
	void updateSummary_Success() throws Exception {
		Summary summary = new Summary();
		summary.setSummaryId(SUMMARY_ID);
		summary.setRecordingId(RECORDING_ID);

		Recording recording = new Recording();
		recording.setRecordingId(RECORDING_ID);
		recording.setUserId(TEST_USER_ID);

		UpdateSummaryRequest request = new UpdateSummaryRequest("New Summary Text", null, null);

		when(summaryService.getSummaryById(SUMMARY_ID)).thenReturn(summary);
		when(recordingService.getRecordingById(RECORDING_ID)).thenReturn(recording);

		mockMvc.perform(patch("/api/summaries/{summaryId}", SUMMARY_ID).principal(createAuthentication(TEST_USER_ID))
				.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk()).andExpect(jsonPath("$.formattedSummaryText").value("New Summary Text"));
	}

	@Test
	void updateSummary_Forbidden() throws Exception {
		Summary summary = new Summary();
		summary.setSummaryId(SUMMARY_ID);
		summary.setRecordingId(RECORDING_ID);

		Recording recording = new Recording();
		recording.setRecordingId(RECORDING_ID);
		recording.setUserId(OTHER_USER_ID); // Different user

		UpdateSummaryRequest request = new UpdateSummaryRequest("New Summary Text", null, null);

		when(summaryService.getSummaryById(SUMMARY_ID)).thenReturn(summary);
		when(recordingService.getRecordingById(RECORDING_ID)).thenReturn(recording);

		mockMvc.perform(patch("/api/summaries/{summaryId}", SUMMARY_ID).principal(createAuthentication(TEST_USER_ID))
				.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isForbidden());
	}

	@Test
	void updateSummary_NotFound() throws Exception {
		UpdateSummaryRequest request = new UpdateSummaryRequest("New Summary Text", null, null);

		when(summaryService.getSummaryById(SUMMARY_ID)).thenReturn(null);

		mockMvc.perform(patch("/api/summaries/{summaryId}", SUMMARY_ID).principal(createAuthentication(TEST_USER_ID))
				.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isNotFound());
	}
}
