package edu.cit.audioscholar.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import edu.cit.audioscholar.dto.analytics.ActivityStatsDto;
import edu.cit.audioscholar.dto.analytics.AnalyticsOverviewDto;
import edu.cit.audioscholar.dto.analytics.ContentEngagementDto;
import edu.cit.audioscholar.dto.analytics.UserDistributionDto;
import edu.cit.audioscholar.service.AnalyticsService;

@SpringBootTest
@AutoConfigureMockMvc
public class AnalyticsControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private AnalyticsService analyticsService;

	@Test
	@WithMockUser(username = "admin", roles = {"ADMIN"})
	public void testGetOverview() throws Exception {
		AnalyticsOverviewDto mockDto = new AnalyticsOverviewDto(100, 50, 102400, 3600);
		when(analyticsService.getOverviewStats()).thenReturn(mockDto);

		mockMvc.perform(get("/api/admin/analytics/overview").contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andExpect(jsonPath("$.totalUsers").value(100))
				.andExpect(jsonPath("$.totalRecordings").value(50));
	}

	@Test
	@WithMockUser(username = "admin", roles = {"ADMIN"})
	public void testGetActivity() throws Exception {
		ActivityStatsDto mockDto = new ActivityStatsDto(Map.of("2023-10-01", 5L), Map.of("2023-10-01", 10L));
		when(analyticsService.getActivityStats()).thenReturn(mockDto);

		mockMvc.perform(get("/api/admin/analytics/activity").contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andExpect(jsonPath("$.newUsersLast30Days['2023-10-01']").value(5));
	}

	@Test
	@WithMockUser(username = "admin", roles = {"ADMIN"})
	public void testGetUserDistribution() throws Exception {
		UserDistributionDto mockDto = new UserDistributionDto(Map.of("google", 80L), Map.of("ROLE_USER", 90L));
		when(analyticsService.getUserDistribution()).thenReturn(mockDto);

		mockMvc.perform(get("/api/admin/analytics/users/distribution").contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andExpect(jsonPath("$.usersByProvider.google").value(80));
	}

	@Test
	@WithMockUser(username = "admin", roles = {"ADMIN"})
	public void testGetContentEngagement() throws Exception {
		ContentEngagementDto dto = new ContentEngagementDto("rec1", "Test Title", 10);
		when(analyticsService.getContentEngagement()).thenReturn(List.of(dto));

		mockMvc.perform(get("/api/admin/analytics/content/engagement").contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andExpect(jsonPath("$[0].recordingId").value("rec1"))
				.andExpect(jsonPath("$[0].favoriteCount").value(10));
	}

	@Test
	@WithMockUser(username = "user", roles = {"USER"})
	public void testAccessDeniedForNonAdmin() throws Exception {
		mockMvc.perform(get("/api/admin/analytics/overview").contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isForbidden());
	}
}
