package edu.cit.audioscholar.service;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

@Service
public class GeminiSmartRotationService {

	private static final Logger logger = LoggerFactory.getLogger(GeminiSmartRotationService.class);

	private final List<String> modelHierarchy;
	private final long baseBackoff;
	private final long maxBackoff;
	private final double backoffMultiplier;

	public GeminiSmartRotationService(@Value("${gemini.model-hierarchy}") String hierarchyStr,
			@Value("${gemini.rotation.base-backoff-ms:2000}") long baseBackoff,
			@Value("${gemini.rotation.max-backoff-ms:60000}") long maxBackoff,
			@Value("${gemini.rotation.backoff-multiplier:2.0}") double backoffMultiplier) {
		// Parse the comma-separated string into a List
		this.modelHierarchy = Arrays.asList(hierarchyStr.split(","));
		this.baseBackoff = baseBackoff;
		this.maxBackoff = maxBackoff;
		this.backoffMultiplier = backoffMultiplier;
	}

	/**
	 * Executes the Gemini API call with Infinite Smart Rotation.
	 *
	 * @param apiCallFunction
	 *            A function that takes the MODEL_NAME as input and returns the
	 *            result (T).
	 * @return The result of type T.
	 */
	public <T> T executeWithInfiniteRotation(Function<String, T> apiCallFunction) {
		long currentBackoff = baseBackoff;
		int hierarchyIndex = 0;
		int cycleCount = 0;

		// INFINITE LOOP: Will only exit on success or non-retriable error
		while (true) {
			String currentModel = modelHierarchy.get(hierarchyIndex);

			try {
				// Try the current model
				// logger.debug("Attempting operation using model: {}", currentModel); //
				// Optional: reduce noise
				return apiCallFunction.apply(currentModel);

			} catch (HttpClientErrorException.TooManyRequests | HttpServerErrorException.ServiceUnavailable e) {
				// 429 or 503 -> Rate Limit or Overloaded.
				// DO NOT SLEEP YET. IMMEDIATE ROTATION.
				logger.warn("Model {} is rate limited/overloaded ({}). Switching to next...", currentModel,
						e.getStatusCode());

			} catch (Exception e) {
				// Check for wrapped 429/503 in unexpected wrapper exceptions if necessary
				// For now, assume standard Spring RestTemplate exceptions or those thrown by
				// apiCallFunction

				// If it's a critical error (400, 401, etc.), rethrow
				logger.error("Non-retriable error on model {}. Stopping rotation.", currentModel, e);
				throw e;
			}

			// --- Rotation Logic ---
			hierarchyIndex++;

			// Did we exhaust the whole list?
			if (hierarchyIndex >= modelHierarchy.size()) {
				cycleCount++;

				// Calculate Backoff
				logger.info("Cycle {} complete. All models exhausted. Backing off...", cycleCount);

				try {
					logger.info("Sleeping for {} ms before restarting hierarchy...", currentBackoff);
					Thread.sleep(currentBackoff);
				} catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
					throw new RuntimeException("Rotation interrupted", ie);
				}

				// Update backoff for next time (Exponential, capped at 60s)
				currentBackoff = (long) Math.min(currentBackoff * backoffMultiplier, maxBackoff);

				// Reset to top of hierarchy
				hierarchyIndex = 0;
				logger.info("Restarting hierarchy at index 0 ({})", modelHierarchy.get(0));
			}
		}
	}
}
