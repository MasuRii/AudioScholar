package edu.cit.audioscholar.util;

import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class RobustTaskExecutor {

	private static final Logger log = LoggerFactory.getLogger(RobustTaskExecutor.class);

	/**
	 * Executes a task indefinitely until it succeeds. Catches ALL exceptions to
	 * prevent the process from terminating.
	 */
	public <T> T executeWithInfiniteRetry(String contextId, String taskDescription, Supplier<T> task) {
		long delayMs = 2000; // Start with 2 seconds
		long maxDelayMs = 60000; // Max wait 1 minute

		while (true) {
			try {
				return task.get();
			} catch (Exception e) {
				log.error("[{}] Failed to {}. Retrying in {}ms. Error: {}", contextId, taskDescription, delayMs,
						e.getMessage());

				// Optional: Add specific logic here if you want to break on IRRECOVERABLE
				// errors
				// (e.g., file completely deleted from DB), otherwise keep looping.

				try {
					Thread.sleep(delayMs);
				} catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
					throw new RuntimeException("Thread interrupted during robust retry", ie);
				}

				// Exponential backoff with cap
				delayMs = Math.min(delayMs * 2, maxDelayMs);
			}
		}
	}

	// Overload for void tasks
	public void executeWithInfiniteRetry(String contextId, String taskDescription, Runnable task) {
		executeWithInfiniteRetry(contextId, taskDescription, () -> {
			task.run();
			return null;
		});
	}
}
