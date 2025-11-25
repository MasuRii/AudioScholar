package edu.cit.audioscholar.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient;

import com.google.firebase.auth.AuthErrorCode;
import com.google.firebase.auth.FirebaseAuthException;

import edu.cit.audioscholar.dto.AuthResponse;
import edu.cit.audioscholar.dto.RegistrationRequest;
import edu.cit.audioscholar.model.User;
import edu.cit.audioscholar.service.FirebaseService;
import edu.cit.audioscholar.service.UserService;

@ExtendWith(MockitoExtension.class)
public class AuthControllerTest {

	@Mock
	private FirebaseService firebaseService;

	@Mock
	private UserService userService;

	@Mock
	private edu.cit.audioscholar.security.JwtTokenProvider jwtTokenProvider;

	@Mock
	private WebClient.Builder webClientBuilder;

	@Mock
	private edu.cit.audioscholar.service.TokenRevocationService tokenRevocationService;

	@Mock
	private edu.cit.audioscholar.service.GitHubApiService gitHubApiService;

	@InjectMocks
	private AuthController authController;

	@Test
	public void testRegisterUser_Success() throws Exception {
		RegistrationRequest request = new RegistrationRequest();
		request.setEmail("test@example.com");
		request.setPassword("password");

		User user = new User();
		user.setUserId("testUid");

		when(userService.registerNewUser(request)).thenReturn(user);
		when(firebaseService.createCustomToken("testUid")).thenReturn("testToken");

		ResponseEntity<?> responseEntity = authController.registerUser(request);

		assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());
		AuthResponse authResponse = (AuthResponse) responseEntity.getBody();
		assertNotNull(authResponse);
		assertEquals(true, authResponse.isSuccess());
		assertEquals("testToken", authResponse.getCustomToken());
	}

	@Test
	public void testRegisterUser_EmailExists() throws Exception {
		RegistrationRequest request = new RegistrationRequest();
		request.setEmail("test@example.com");
		request.setPassword("password");

		FirebaseAuthException exception = mock(FirebaseAuthException.class);
		when(exception.getAuthErrorCode()).thenReturn(AuthErrorCode.EMAIL_ALREADY_EXISTS);
		when(userService.registerNewUser(request)).thenThrow(exception);

		ResponseEntity<?> responseEntity = authController.registerUser(request);

		assertEquals(HttpStatus.CONFLICT, responseEntity.getStatusCode());
	}
}
