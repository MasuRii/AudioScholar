package edu.cit.audioscholar.dto.analytics;

import java.util.Map;

/**
 * DTO for user distribution statistics.
 */
public class UserDistributionDto {
	private Map<String, Long> usersByProvider;
	private Map<String, Long> usersByRole;

	public UserDistributionDto() {
	}

	public UserDistributionDto(Map<String, Long> usersByProvider, Map<String, Long> usersByRole) {
		this.usersByProvider = usersByProvider;
		this.usersByRole = usersByRole;
	}

	public Map<String, Long> getUsersByProvider() {
		return usersByProvider;
	}

	public void setUsersByProvider(Map<String, Long> usersByProvider) {
		this.usersByProvider = usersByProvider;
	}

	public Map<String, Long> getUsersByRole() {
		return usersByRole;
	}

	public void setUsersByRole(Map<String, Long> usersByRole) {
		this.usersByRole = usersByRole;
	}
}
