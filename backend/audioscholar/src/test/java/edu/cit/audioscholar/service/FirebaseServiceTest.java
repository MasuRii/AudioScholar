package edu.cit.audioscholar.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;

@ExtendWith(MockitoExtension.class)
public class FirebaseServiceTest {

	@Mock
	private FirebaseAuth firebaseAuth;

	@Mock
	private UserRecord userRecord;

	@Spy
	@InjectMocks
	private FirebaseService firebaseService;

	@Test
	public void testCreateCustomToken_Success() throws FirebaseAuthException {
		doReturn(firebaseAuth).when(firebaseService).getFirebaseAuth();
		String uid = "testUid";
		String expectedToken = "testToken";

		when(firebaseAuth.getUser(uid)).thenReturn(userRecord);
		when(firebaseAuth.createCustomToken(uid)).thenReturn(expectedToken);

		String actualToken = firebaseService.createCustomToken(uid);

		assertEquals(expectedToken, actualToken);
	}

	@Test
	public void testCreateCustomToken_BlankUid() {
		assertThrows(IllegalArgumentException.class, () -> {
			firebaseService.createCustomToken("");
		});
	}

	@Test
	public void testCreateCustomToken_UserNotFound() throws FirebaseAuthException {
		doReturn(firebaseAuth).when(firebaseService).getFirebaseAuth();
		String uid = "nonExistentUid";

		when(firebaseAuth.getUser(uid)).thenThrow(FirebaseAuthException.class);

		assertThrows(FirebaseAuthException.class, () -> {
			firebaseService.createCustomToken(uid);
		});
	}
}
