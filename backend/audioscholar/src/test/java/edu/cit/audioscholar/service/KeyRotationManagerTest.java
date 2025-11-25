package edu.cit.audioscholar.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import edu.cit.audioscholar.model.KeyProvider;

class KeyRotationManagerTest {

	private KeyRotationManagerImpl keyRotationManager;

	@BeforeEach
	void setUp() {
		keyRotationManager = new KeyRotationManagerImpl();
	}

	@Test
	void testKeyRotation_RoundRobin() {
		// Setup: 3 keys
		ReflectionTestUtils.setField(keyRotationManager, "geminiKeysRaw", "key1,key2,key3");
		ReflectionTestUtils.setField(keyRotationManager, "geminiKeyLegacy", "");
		ReflectionTestUtils.setField(keyRotationManager, "convertApiSecretsRaw", "");
		ReflectionTestUtils.setField(keyRotationManager, "convertApiSecretLegacy", "secret1");

		keyRotationManager.init();

		// Test Gemini Rotation
		String k1 = keyRotationManager.getKey(KeyProvider.GEMINI);
		String k2 = keyRotationManager.getKey(KeyProvider.GEMINI);
		String k3 = keyRotationManager.getKey(KeyProvider.GEMINI);
		String k4 = keyRotationManager.getKey(KeyProvider.GEMINI);

		assertEquals("key1", k1);
		assertEquals("key2", k2);
		assertEquals("key3", k3);
		assertEquals("key1", k4); // loops back
	}

	@Test
	void testLegacyKeyFallback() {
		ReflectionTestUtils.setField(keyRotationManager, "geminiKeysRaw", "");
		ReflectionTestUtils.setField(keyRotationManager, "geminiKeyLegacy", "legacyKey");
		ReflectionTestUtils.setField(keyRotationManager, "convertApiSecretsRaw", "");
		ReflectionTestUtils.setField(keyRotationManager, "convertApiSecretLegacy", "");

		keyRotationManager.init();

		String key = keyRotationManager.getKey(KeyProvider.GEMINI);
		assertEquals("legacyKey", key);
	}

	@Test
	void testCooldownLogic() {
		// Setup: 2 keys
		ReflectionTestUtils.setField(keyRotationManager, "geminiKeysRaw", "keyA,keyB");
		ReflectionTestUtils.setField(keyRotationManager, "geminiKeyLegacy", "");
		ReflectionTestUtils.setField(keyRotationManager, "convertApiSecretsRaw", "");
		ReflectionTestUtils.setField(keyRotationManager, "convertApiSecretLegacy", "");

		keyRotationManager.init();

		// 1. Get keyA
		String firstKey = keyRotationManager.getKey(KeyProvider.GEMINI);
		assertEquals("keyA", firstKey);

		// 2. Report 429 on keyA
		keyRotationManager.reportError(KeyProvider.GEMINI, "keyA", 429);

		// 3. Next calls should skip keyA and return keyB
		String secondKey = keyRotationManager.getKey(KeyProvider.GEMINI); // Should be keyB
		assertEquals("keyB", secondKey);

		String thirdKey = keyRotationManager.getKey(KeyProvider.GEMINI); // Should loop to keyA, see it's cooldown, go
																			// to keyB
		assertEquals("keyB", thirdKey);
	}

	@Test
	void testAllKeysInCooldown() {
		// Setup: 2 keys
		ReflectionTestUtils.setField(keyRotationManager, "geminiKeysRaw", "keyX,keyY");
		ReflectionTestUtils.setField(keyRotationManager, "geminiKeyLegacy", "");
		ReflectionTestUtils.setField(keyRotationManager, "convertApiSecretsRaw", "");
		ReflectionTestUtils.setField(keyRotationManager, "convertApiSecretLegacy", "");

		keyRotationManager.init();

		// Put both in cooldown
		keyRotationManager.reportError(KeyProvider.GEMINI, "keyX", 429);
		keyRotationManager.reportError(KeyProvider.GEMINI, "keyY", 429);

		// Should still return a key (fallback behavior) rather than failing
		String forcedKey = keyRotationManager.getKey(KeyProvider.GEMINI);
		assertNotNull(forcedKey);
		assertTrue(forcedKey.equals("keyX") || forcedKey.equals("keyY"));
	}

	@Test
	void testConvertAPIConfig() {
		ReflectionTestUtils.setField(keyRotationManager, "geminiKeysRaw", "");
		ReflectionTestUtils.setField(keyRotationManager, "geminiKeyLegacy", "");
		ReflectionTestUtils.setField(keyRotationManager, "convertApiSecretsRaw", "sec1,sec2");
		ReflectionTestUtils.setField(keyRotationManager, "convertApiSecretLegacy", "secLegacy"); // should be added if
																									// not duplicate

		keyRotationManager.init();

		Set<String> receivedSecrets = new HashSet<>();
		for (int i = 0; i < 10; i++) {
			receivedSecrets.add(keyRotationManager.getKey(KeyProvider.CONVERTAPI));
		}

		assertTrue(receivedSecrets.contains("sec1"));
		assertTrue(receivedSecrets.contains("sec2"));
		assertTrue(receivedSecrets.contains("secLegacy"));
	}

	@Test
	void testBackwardCompatibility_GeminiLegacyOnly() {
		// Setup: Only legacy Gemini key is provided
		ReflectionTestUtils.setField(keyRotationManager, "geminiKeysRaw", "");
		ReflectionTestUtils.setField(keyRotationManager, "geminiKeyLegacy", "gemini_legacy_key");
		ReflectionTestUtils.setField(keyRotationManager, "convertApiSecretsRaw", "");
		ReflectionTestUtils.setField(keyRotationManager, "convertApiSecretLegacy", "");

		keyRotationManager.init();

		// Verification
		String key = keyRotationManager.getKey(KeyProvider.GEMINI);
		assertEquals("gemini_legacy_key", key);
	}

	@Test
	void testBackwardCompatibility_ConvertApiLegacyOnly() {
		// Setup: Only legacy ConvertAPI secret is provided
		ReflectionTestUtils.setField(keyRotationManager, "geminiKeysRaw", "");
		ReflectionTestUtils.setField(keyRotationManager, "geminiKeyLegacy", "");
		ReflectionTestUtils.setField(keyRotationManager, "convertApiSecretsRaw", "");
		ReflectionTestUtils.setField(keyRotationManager, "convertApiSecretLegacy", "convertapi_legacy_secret");

		keyRotationManager.init();

		// Verification
		String key = keyRotationManager.getKey(KeyProvider.CONVERTAPI);
		assertEquals("convertapi_legacy_secret", key);
	}
}
