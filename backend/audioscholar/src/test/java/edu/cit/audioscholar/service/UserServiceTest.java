package edu.cit.audioscholar.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.firebase.auth.UserRecord;

import edu.cit.audioscholar.dto.RegistrationRequest;
import edu.cit.audioscholar.model.User;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

	@Mock
	private FirebaseService firebaseService;

	@Mock
	private NhostStorageService nhostStorageService;

	private UserService userService;

	@BeforeEach
	void setUp() {
		// Manually instantiate to avoid file system operations in constructor during
		// test
		userService = new UserService(firebaseService, nhostStorageService, ".");
	}

	@Test
	void testFindOrCreateUser_NewUser_EmailExists_ShouldMigrate() throws Exception {
		// Scenario:
		// Firebase Auth returns a new UID (newUid) for an email.
		// Firestore already has a user with this email but under a different UID
		// (oldUid).

		String email = "test@example.com";
		String newUid = "new-uid-123";
		String oldUid = "old-uid-456";
		String name = "Test User";
		String provider = "google";
		String providerId = "google-id";
		String photoUrl = "http://google.com/pic.jpg";

		// 1. getUserById(newUid) returns null (User doesn't exist under new UID yet)
		when(firebaseService.getData("users", newUid)).thenReturn(null);

		// 2. findUserByEmail(email) returns existing user (User exists under old UID)
		Map<String, Object> existingUserData = Map.of("userId", oldUid, "email", email, "displayName", "Old Name",
				"provider", "github", "profileImageUrl", "http://github.com/pic.jpg", "roles", List.of("ROLE_USER"),
				"recordingIds", Collections.emptyList(), "favoriteRecordingIds", Collections.emptyList());
		when(firebaseService.queryCollection("users", "email", email)).thenReturn(List.of(existingUserData));

		// Mock save/delete (They return String, not void)
		when(firebaseService.saveData(eq("users"), eq(newUid), anyMap())).thenReturn("timestamp");
		when(firebaseService.deleteData("users", oldUid)).thenReturn("timestamp");

		// Act
		User result = userService.findOrCreateUserByFirebaseDetails(newUid, email, name, provider, providerId,
				photoUrl);

		// Assert
		assertNotNull(result);
		assertEquals(newUid, result.getUserId(), "User ID should be updated to the new UID");
		assertEquals(email, result.getEmail());
		assertEquals(name, result.getDisplayName(), "Display name should be updated from new provider");
		assertEquals(photoUrl, result.getProfileImageUrl(), "Profile image should be updated");

		// Verify migration steps
		verify(firebaseService).deleteData("users", oldUid); // Should delete old record
		verify(firebaseService).saveData(eq("users"), eq(newUid), anyMap()); // Should save new record
	}

	@Test
	void testFindOrCreateUser_ProfileImage_PreserveManualUpload() throws Exception {
		// Scenario: Existing user has a manually uploaded image (Nhost).
		// New login comes with a provider image. Should NOT overwrite the manual image.

		String uid = "uid-123";
		String email = "test@example.com";
		String name = "Test User";
		String provider = "google";
		String providerId = "google-id";
		String newPhotoUrl = "http://google.com/pic.jpg";
		String manualPhotoUrl = "https://backend-blah.nhost.run/v1/files/file-id";

		// Mock checking if it's an nhost url. Use lenient() because logic might not be
		// implemented yet or called if conditions vary.
		lenient().when(nhostStorageService.isNhostUrl(manualPhotoUrl)).thenReturn(true);

		Map<String, Object> existingUserData = Map.of("userId", uid, "email", email, "displayName", name, "provider",
				"email", "profileImageUrl", manualPhotoUrl, "roles", List.of("ROLE_USER"));

		// User exists by UID
		when(firebaseService.getData("users", uid)).thenReturn(existingUserData);

		// Act
		User result = userService.findOrCreateUserByFirebaseDetails(uid, email, name, provider, providerId,
				newPhotoUrl);

		// Assert
		assertEquals(manualPhotoUrl, result.getProfileImageUrl(), "Should preserve manual upload");
		// Verify saveData was NOT called (no update needed)
		verify(firebaseService, times(0)).saveData(eq("users"), eq(uid), anyMap());
	}

	@Test
	void testRegisterNewUser_UserExistsInFirestore_ShouldMigrate() throws Exception {
		// Scenario: User exists in Firestore (e.g., created via OAuth with random UUID)
		// User tries to register with Email/Password.
		// Should migrate the existing Firestore profile to use the new Firebase UID.

		String email = "existing@example.com";
		String password = "password123";
		String oldUid = "oauth-generated-uuid";
		String newFirebaseUid = "firebase-uid-789";
		String firstName = "New";
		String lastName = "User";

		RegistrationRequest request = new RegistrationRequest();
		request.setEmail(email);
		request.setPassword(password);
		request.setFirstName(firstName);
		request.setLastName(lastName);

		// Mock existing user in Firestore
		User existingUser = new User();
		existingUser.setUserId(oldUid);
		existingUser.setEmail(email);
		existingUser.setDisplayName("Old Name");
		existingUser.setProvider("google");
		existingUser.setRoles(List.of("ROLE_USER"));
		Map<String, Object> existingUserMap = existingUser.toMap();

		when(firebaseService.queryCollection("users", "email", email)).thenReturn(List.of(existingUserMap));

		// Mock Firebase User Creation
		UserRecord mockUserRecord = mock(UserRecord.class);
		when(mockUserRecord.getUid()).thenReturn(newFirebaseUid);
		when(firebaseService.createFirebaseUser(eq(email), eq(password), anyString())).thenReturn(mockUserRecord);

		// Mock save/delete
		when(firebaseService.saveData(eq("users"), eq(newFirebaseUid), anyMap())).thenReturn("timestamp");
		when(firebaseService.deleteData("users", oldUid)).thenReturn("timestamp");

		// Act
		User result = userService.registerNewUser(request);

		// Assert
		assertNotNull(result);
		assertEquals(newFirebaseUid, result.getUserId());
		assertEquals("email", result.getProvider()); // Provider should update to email

		verify(firebaseService).deleteData("users", oldUid);
		verify(firebaseService).saveData(eq("users"), eq(newFirebaseUid), anyMap());
	}
}
