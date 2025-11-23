package edu.cit.audioscholar.service;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.cit.audioscholar.model.KeyProvider;

@Service
public class ConvertApiService {

	private static final Logger log = LoggerFactory.getLogger(ConvertApiService.class);
	private static final String CONVERT_API_URL = "https://v2.convertapi.com/convert/pptx/to/pdf";
	private static final int MAX_RETRIES = 3;

	private final RestTemplate restTemplate;
	private final KeyRotationManager keyRotationManager;
	private final ObjectMapper objectMapper;

	public ConvertApiService(RestTemplate restTemplate, KeyRotationManager keyRotationManager) {
		this.restTemplate = restTemplate;
		this.keyRotationManager = keyRotationManager;
		this.objectMapper = new ObjectMapper();
	}

	public String convertPptxUrlToPdfUrl(String pptxUrl) throws Exception {
		if (pptxUrl == null || pptxUrl.isBlank()) {
			throw new IllegalArgumentException("PPTX URL cannot be null or empty");
		}

		log.info("Starting PPTX to PDF conversion for file: {}", pptxUrl);

		Exception lastException = null;

		for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
			String secret = null;
			try {
				secret = keyRotationManager.getKey(KeyProvider.CONVERTAPI);
			} catch (Exception e) {
				log.error("Failed to get ConvertAPI key", e);
				throw e;
			}

			try {
				String resultUrl = executeConversion(pptxUrl, secret);
				keyRotationManager.reportSuccess(KeyProvider.CONVERTAPI, secret);
				log.info("PDF conversion successful on attempt {}.", attempt);
				return resultUrl;
			} catch (HttpClientErrorException e) {
				lastException = e;
				int statusCode = e.getStatusCode().value();
				log.warn("ConvertAPI failed with status {} on attempt {}/{}", statusCode, attempt, MAX_RETRIES);

				keyRotationManager.reportError(KeyProvider.CONVERTAPI, secret, statusCode);

				if (statusCode == 429 || statusCode == 403) {
					// Rate limited or forbidden (quota exceeded), try next key in next iteration
					continue;
				} else {
					// Other client error, probably fatal (e.g. 400 bad request)
					throw e;
				}
			} catch (RestClientException e) {
				lastException = e;
				log.error("ConvertAPI connection error on attempt {}/{}", attempt, MAX_RETRIES, e);
				// Network error, treat as retryable if we have attempts left
			} catch (Exception e) {
				lastException = e;
				log.error("Unexpected error on attempt {}/{}", attempt, MAX_RETRIES, e);
				throw e;
			}
		}

		throw new Exception("Failed to convert PPTX to PDF after " + MAX_RETRIES + " attempts", lastException);
	}

	private String executeConversion(String pptxUrl, String secret) throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		// Construct Body: { "Parameters": [ { "Name": "File", "FileValue": { "Url":
		// "..." } }, { "Name": "StoreFile", "Value": true } ] }

		Map<String, Object> fileValue = Map.of("Url", pptxUrl);
		Map<String, Object> fileParam = Map.of("Name", "File", "FileValue", fileValue);
		Map<String, Object> storeFileParam = Map.of("Name", "StoreFile", "Value", true);

		Map<String, Object> body = Map.of("Parameters", List.of(fileParam, storeFileParam));

		HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);

		String url = CONVERT_API_URL + "?Secret=" + secret;

		ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);

		if (!response.getStatusCode().is2xxSuccessful()) {
			throw new RuntimeException("ConvertAPI returned non-success status: " + response.getStatusCode());
		}

		JsonNode root = objectMapper.readTree(response.getBody());
		// Response structure: { "Files": [ { "Url": "..." } ] }
		if (root.has("Files") && root.get("Files").isArray() && root.get("Files").size() > 0) {
			return root.get("Files").get(0).get("Url").asText();
		}

		throw new RuntimeException("Invalid response from ConvertAPI: " + response.getBody());
	}
}
