package edu.cit.audioscholar.dto;

import java.util.List;
import java.util.Map;

public class UpdateSummaryRequest {

	private String formattedSummaryText;
	private List<String> keyPoints;
	private List<Map<String, String>> glossary;

	public UpdateSummaryRequest() {
	}

	public UpdateSummaryRequest(String formattedSummaryText, List<String> keyPoints,
			List<Map<String, String>> glossary) {
		this.formattedSummaryText = formattedSummaryText;
		this.keyPoints = keyPoints;
		this.glossary = glossary;
	}

	public String getFormattedSummaryText() {
		return formattedSummaryText;
	}

	public void setFormattedSummaryText(String formattedSummaryText) {
		this.formattedSummaryText = formattedSummaryText;
	}

	public List<String> getKeyPoints() {
		return keyPoints;
	}

	public void setKeyPoints(List<String> keyPoints) {
		this.keyPoints = keyPoints;
	}

	public List<Map<String, String>> getGlossary() {
		return glossary;
	}

	public void setGlossary(List<Map<String, String>> glossary) {
		this.glossary = glossary;
	}
}
