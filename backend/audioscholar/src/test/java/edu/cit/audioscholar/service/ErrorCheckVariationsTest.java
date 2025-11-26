package edu.cit.audioscholar.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class ErrorCheckVariationsTest {

	@Test
	public void testVariations() throws Exception {
		// Standard
		String standard = "{\"error\":\"msg\",\"details\":\"det\"}";
		assertTrue(check(standard), "Standard should pass");

		// Spaces
		String spaces = "{\"error\" : \"msg\", \"details\" : \"det\"}";
		assertTrue(check(spaces), "Spaces should pass");

		// Newlines
		String newlines = "{\n  \"error\": \"msg\",\n  \"details\": \"det\"\n}";
		assertTrue(check(newlines), "Newlines should pass");

		// Case sensitivity - The check is case sensitive!
		String caps = "{\"Error\":\"msg\",\"Details\":\"det\"}";
		// If createErrorResponse used caps, this would fail the check.
		// But createErrorResponse uses lowercase "error" and "details".

		// What if keys are different?
		String diffKeys = "{\"err\":\"msg\",\"dtl\":\"det\"}";
		assertFalse(check(diffKeys), "Different keys should fail");

		// What if details is missing?
		String noDetails = "{\"error\":\"msg\"}";
		assertFalse(check(noDetails), "Missing details should fail");

		// What if error is missing?
		String noError = "{\"details\":\"msg\"}";
		assertFalse(check(noError), "Missing error should fail");

		// The user provided string
		String userString = "{\"error\":\"API Client Error: 403 FORBIDDEN\",\"details\":\"Your API key was reported as leaked. Please use another API key.\"}";
		assertTrue(check(userString), "User string must pass");
	}

	private boolean check(String json) {
		return json.contains("\"error\"") && json.contains("\"details\"");
	}
}
