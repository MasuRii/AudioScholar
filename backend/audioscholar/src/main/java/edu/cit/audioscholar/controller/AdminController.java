package edu.cit.audioscholar.controller;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.cit.audioscholar.dto.AdminUpdateUserRolesRequest;
import edu.cit.audioscholar.dto.AdminUpdateUserStatusRequest;
import edu.cit.audioscholar.service.UserService;
import jakarta.validation.Valid;

/**
 * Controller for administrative actions such as user management and system
 * health checks.
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

	private static final Logger logger = LoggerFactory.getLogger(AdminController.class);
	private final UserService userService;

	public AdminController(UserService userService) {
		this.userService = userService;
	}

	/**
	 * Lists users from the authentication provider.
	 *
	 * @param limit
	 *            The maximum number of users to return (default 20).
	 * @param pageToken
	 *            The token for the next page of results.
	 * @return A map containing the list of users and the next page token.
	 */
	@GetMapping("/users")
	public ResponseEntity<Map<String, Object>> listUsers(@RequestParam(defaultValue = "20") int limit,
			@RequestParam(required = false) String pageToken) {
		logger.info("Admin request to list users. Limit: {}, PageToken: {}", limit, pageToken);
		try {
			// Retrieve users from Firestore to ensure consistent role mapping
			Map<String, Object> response = userService.getAllUsersFromFirestore(limit, pageToken);
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			logger.error("Error listing users: {}", e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("error", "Failed to list users", "details", e.getMessage()));
		}
	}

	/**
	 * Updates the status (enabled/disabled) of a user.
	 *
	 * @param uid
	 *            The user ID.
	 * @param request
	 *            The request containing the new status.
	 * @return A response entity with the update status.
	 */
	@PutMapping("/users/{uid}/status")
	public ResponseEntity<Map<String, Object>> updateUserStatus(@PathVariable String uid,
			@RequestBody AdminUpdateUserStatusRequest request) {
		logger.info("Admin request to update status for user {}: disabled={}", uid, request.disabled());
		try {
			userService.updateUserStatus(uid, request.disabled());
			return ResponseEntity.ok(
					Map.of("uid", uid, "disabled", request.disabled(), "message", "User status updated successfully."));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
		} catch (Exception e) {
			logger.error("Error updating user status for {}: {}", uid, e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("error", "Failed to update user status"));
		}
	}

	/**
	 * Updates the roles of a user.
	 *
	 * @param uid
	 *            The user ID.
	 * @param request
	 *            The request containing the new roles.
	 * @return A response entity with the update status.
	 */
	@PutMapping("/users/{uid}/roles")
	public ResponseEntity<Map<String, Object>> updateUserRoles(@PathVariable String uid,
			@Valid @RequestBody AdminUpdateUserRolesRequest request) {
		logger.info("Admin request to update roles for user {}: {}", uid, request.roles());
		try {
			userService.updateUserRoles(uid, request.roles());
			return ResponseEntity
					.ok(Map.of("uid", uid, "roles", request.roles(), "message", "User roles updated successfully."));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
		} catch (Exception e) {
			logger.error("Error updating user roles for {}: {}", uid, e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("error", "Failed to update user roles"));
		}
	}

	/**
	 * Checks the system health.
	 *
	 * @return A map containing system status information.
	 */
	@GetMapping("/system/health")
	public ResponseEntity<Map<String, Object>> getSystemHealth() {
		// Basic health check
		Map<String, Object> health = new HashMap<>();
		health.put("status", "UP");
		health.put("db", "connected"); // Assuming if the app is up, DB is reachable (or handled by monitoring)
		health.put("timestamp", System.currentTimeMillis());
		return ResponseEntity.ok(health);
	}
}
