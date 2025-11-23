package edu.cit.audioscholar.service;

import edu.cit.audioscholar.model.KeyProvider;

public interface KeyRotationManager {
	/**
	 * Retrieves the next available API key for the specified provider.
	 * Implementations should skip keys that are currently in cooldown.
	 *
	 * @param provider
	 *            The service provider (GEMINI, CONVERTAPI)
	 * @return A valid API key
	 * @throws RuntimeException
	 *             if no keys are available
	 */
	String getKey(KeyProvider provider);

	/**
	 * Reports an error for a specific key. If the statusCode indicates a rate limit
	 * (e.g., 429, 403), the key may be placed in cooldown.
	 *
	 * @param provider
	 *            The service provider
	 * @param key
	 *            The key that caused the error
	 * @param statusCode
	 *            The HTTP status code returned by the API
	 */
	void reportError(KeyProvider provider, String key, int statusCode);

	/**
	 * Reports a successful usage of a key. Useful for metrics or resetting failure
	 * counts if needed.
	 *
	 * @param provider
	 *            The service provider
	 * @param key
	 *            The key that was successfully used
	 */
	void reportSuccess(KeyProvider provider, String key);
}
