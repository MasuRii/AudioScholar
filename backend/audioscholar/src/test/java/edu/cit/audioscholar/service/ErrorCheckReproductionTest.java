package edu.cit.audioscholar.service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class ErrorCheckReproductionTest {

	@Test
	public void testErrorCheck() throws Exception {
		ObjectMapper objectMapper = new ObjectMapper();

		// Simulate GeminiService.createErrorResponse
		String errorTitle = "API Client Error: 403 FORBIDDEN";
		String details = "Your API key was reported as leaked. Please use another API key.";

		ObjectNode errorResponse = objectMapper.createObjectNode();
		errorResponse.put("error", errorTitle);
		errorResponse.put("details", details);

		String summarizationJson = objectMapper.writeValueAsString(errorResponse);

		System.out.println("Generated JSON: " + summarizationJson);

		// Simulate SummarizationListenerService check
		boolean hasError = summarizationJson.contains("\"error\"") && summarizationJson.contains("\"details\"");

		System.out.println("Has Error: " + hasError);

		assertTrue(hasError, "The error check should have detected the error JSON");
	}
}
