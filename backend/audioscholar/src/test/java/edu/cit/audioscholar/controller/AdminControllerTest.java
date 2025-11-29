package edu.cit.audioscholar.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.auth.ListUsersPage;

import edu.cit.audioscholar.config.SecurityConfig;
import edu.cit.audioscholar.dto.AdminUpdateUserRolesRequest;
import edu.cit.audioscholar.dto.AdminUpdateUserStatusRequest;
import edu.cit.audioscholar.security.JwtTokenProvider;
import edu.cit.audioscholar.service.OAuth2LoginSuccessHandler;
import edu.cit.audioscholar.service.TokenRevocationService;
import edu.cit.audioscholar.service.UserService;

@WebMvcTest(AdminController.class)
@Import({SecurityConfig.class, AdminControllerTest.TestConfig.class})
public class AdminControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockBean
	private UserService userService;

	@MockBean
	private OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

	@MockBean
	private TokenRevocationService tokenRevocationService;

	// Note: JwtTokenProvider is NOT @MockBean here to avoid overriding the
	// configured mock in TestConfig.
	// If we used @MockBean, it would reset the behavior and return null for
	// getJwtSecretKey(),
	// causing SecurityConfig initialization to fail.

	@TestConfiguration
	static class TestConfig {
		@Bean
		@Primary
		public JwtTokenProvider jwtTokenProvider() {
			JwtTokenProvider mock = Mockito.mock(JwtTokenProvider.class);
			// Use a dummy key string of sufficient length for HS256 (32 bytes)
			String secret = "12345678901234567890123456789012";
			SecretKey key = new SecretKeySpec(secret.getBytes(), "HmacSHA256");

			Mockito.when(mock.getJwtSecretKey()).thenReturn(key);
			return mock;
		}
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void listUsers_Success() throws Exception {
		ListUsersPage mockPage = mock(ListUsersPage.class);
		when(mockPage.getValues()).thenReturn(Collections.emptyList());
		when(mockPage.getNextPageToken()).thenReturn(null);
		when(userService.getAllUsers(anyInt(), any())).thenReturn(mockPage);

		mockMvc.perform(get("/api/admin/users").contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
	}

	@Test
	@WithMockUser(roles = "USER")
	void listUsers_Forbidden() throws Exception {
		mockMvc.perform(get("/api/admin/users").contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void updateUserStatus_Success() throws Exception {
		AdminUpdateUserStatusRequest request = new AdminUpdateUserStatusRequest(true);
		doNothing().when(userService).updateUserStatus(anyString(), anyBoolean());

		mockMvc.perform(put("/api/admin/users/testUid/status").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))).andExpect(status().isOk());
	}

	@Test
	@WithMockUser(roles = "USER")
	void updateUserStatus_Forbidden() throws Exception {
		AdminUpdateUserStatusRequest request = new AdminUpdateUserStatusRequest(true);

		mockMvc.perform(put("/api/admin/users/testUid/status").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))).andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void updateUserRoles_Success() throws Exception {
		AdminUpdateUserRolesRequest request = new AdminUpdateUserRolesRequest(List.of("ROLE_ADMIN"));
		doNothing().when(userService).updateUserRoles(anyString(), anyList());

		mockMvc.perform(put("/api/admin/users/testUid/roles").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))).andExpect(status().isOk());
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void getSystemHealth_Success() throws Exception {
		mockMvc.perform(get("/api/admin/system/health").contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk());
	}
}
