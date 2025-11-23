package edu.cit.audioscholar.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import edu.cit.audioscholar.model.KeyProvider;

@Service
public class KeyRotationManagerImpl implements KeyRotationManager {

	private static final Logger log = LoggerFactory.getLogger(KeyRotationManagerImpl.class);
	private static final long COOLDOWN_DURATION_MS = 60 * 1000; // 1 minute cooldown

	// Storage for keys per provider
	private final Map<KeyProvider, List<String>> keyStore = new ConcurrentHashMap<>();

	// Round-robin counters per provider
	private final Map<KeyProvider, AtomicInteger> counters = new ConcurrentHashMap<>();

	// Cooldown tracking: Key -> Timestamp when it becomes available again
	private final Map<String, Long> cooldownMap = new ConcurrentHashMap<>();

	@Value("${gemini.api.keys:}")
	private String geminiKeysRaw;

	@Value("${google.ai.api.key:}")
	private String geminiKeyLegacy;

	@Value("${convertapi.secrets:}")
	private String convertApiSecretsRaw;

	@Value("${convertapi.secret:${CONVERTAPI_SECRET:}}")
	private String convertApiSecretLegacy;

	@PostConstruct
	public void init() {
		loadKeys(KeyProvider.GEMINI, geminiKeysRaw, geminiKeyLegacy);
		loadKeys(KeyProvider.CONVERTAPI, convertApiSecretsRaw, convertApiSecretLegacy);
	}

	private void loadKeys(KeyProvider provider, String listRaw, String singleLegacy) {
		List<String> keys = new ArrayList<>();

		// 1. Try loading from comma-separated list
		if (listRaw != null && !listRaw.isBlank()) {
			keys.addAll(Arrays.stream(listRaw.split(",")).map(String::trim).filter(s -> !s.isEmpty())
					.collect(Collectors.toList()));
		}

		// 2. Fallback or Merge: If list is empty, or just to be safe, check legacy key
		if (singleLegacy != null && !singleLegacy.isBlank()) {
			// Avoid duplicates
			if (!keys.contains(singleLegacy)) {
				keys.add(singleLegacy);
			}
		}

		if (keys.isEmpty()) {
			log.warn("No API keys found for provider: {}", provider);
		} else {
			log.info("Loaded {} keys for provider: {}", keys.size(), provider);
		}

		keyStore.put(provider, Collections.unmodifiableList(keys));
		counters.put(provider, new AtomicInteger(0));
	}

	@Override
	public String getKey(KeyProvider provider) {
		List<String> keys = keyStore.getOrDefault(provider, Collections.emptyList());
		if (keys.isEmpty()) {
			throw new RuntimeException("No API keys configured for " + provider);
		}

		AtomicInteger counter = counters.get(provider);
		int start = counter.get();
		int size = keys.size();

		// Try to find a non-cooldown key, trying each key at least once
		for (int i = 0; i < size; i++) {
			// Get next index atomically-ish (just round robin)
			// We use getAndIncrement but mod it locally.
			// Race conditions on the exact index don't matter as much as distribution.
			int index = Math.abs(counter.getAndIncrement() % size);
			String candidateKey = keys.get(index);

			if (!isCooldown(candidateKey)) {
				return candidateKey;
			}
		}

		// If all keys are in cooldown, force use of the "next" one anyway
		// or throw exception. For resilience, we usually prefer to return a key
		// and hope the rate limit has passed or we handle the error again.
		// Alternatively, we could wait, but that blocks threads.
		// Let's return the next one in round-robin sequence even if in cooldown,
		// logging a warning.
		log.warn("All keys for {} are in cooldown/rate-limited state. Returning a candidate anyway.", provider);
		int index = Math.abs(counter.getAndIncrement() % size);
		return keys.get(index);
	}

	@Override
	public void reportError(KeyProvider provider, String key, int statusCode) {
		if (isRateLimitError(statusCode)) {
			log.warn("Rate limit error ({}) reported for key: ...{}. Putting in cooldown for {}ms.", statusCode,
					maskKey(key), COOLDOWN_DURATION_MS);
			cooldownMap.put(key, System.currentTimeMillis() + COOLDOWN_DURATION_MS);
		}
	}

	@Override
	public void reportSuccess(KeyProvider provider, String key) {
		// Optional: If we wanted to implement "slow start" or remove from cooldown
		// early (if it was a soft error)
		// For now, we just trust the cooldown timer.
		// We could clear cooldown if a key suddenly works, but usually 429s require
		// time.
	}

	private boolean isCooldown(String key) {
		Long expiry = cooldownMap.get(key);
		if (expiry == null) {
			return false;
		}
		if (System.currentTimeMillis() > expiry) {
			cooldownMap.remove(key);
			return false;
		}
		return true;
	}

	private boolean isRateLimitError(int statusCode) {
		return statusCode == 429 || statusCode == 403;
	}

	private String maskKey(String key) {
		if (key == null || key.length() < 8)
			return "********";
		return key.substring(key.length() - 4);
	}
}
