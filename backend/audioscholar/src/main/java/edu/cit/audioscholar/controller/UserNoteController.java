package edu.cit.audioscholar.controller;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import edu.cit.audioscholar.dto.CreateUserNoteRequest;
import edu.cit.audioscholar.dto.UpdateUserNoteRequest;
import edu.cit.audioscholar.dto.UserNoteDto;
import edu.cit.audioscholar.model.UserNote;
import edu.cit.audioscholar.service.UserNoteService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/notes")
public class UserNoteController {

	private static final Logger log = LoggerFactory.getLogger(UserNoteController.class);
	private final UserNoteService userNoteService;

	public UserNoteController(UserNoteService userNoteService) {
		this.userNoteService = userNoteService;
	}

	@PostMapping
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<UserNoteDto> createNote(Authentication authentication,
			@Valid @RequestBody CreateUserNoteRequest request) {
		String userId = getUserIdFromAuthentication(authentication);
		log.info("Request to create note for user: {} and recording: {}", userId, request.getRecordingId());
		try {
			UserNote note = userNoteService.createNote(userId, request);
			return ResponseEntity.status(HttpStatus.CREATED).body(UserNoteDto.fromModel(note));
		} catch (IllegalArgumentException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
		} catch (ExecutionException | InterruptedException e) {
			log.error("Error creating note", e);
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error creating note", e);
		}
	}

	@GetMapping
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<List<UserNoteDto>> getNotesForRecording(Authentication authentication,
			@RequestParam("recordingId") String recordingId) {
		String userId = getUserIdFromAuthentication(authentication);
		log.info("Request to get notes for user: {} and recording: {}", userId, recordingId);

		List<UserNote> notes = userNoteService.getNotesByRecordingId(userId, recordingId);
		List<UserNoteDto> noteDtos = notes.stream().map(UserNoteDto::fromModel).collect(Collectors.toList());

		return ResponseEntity.ok(noteDtos);
	}

	@GetMapping("/{noteId}")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<UserNoteDto> getNote(Authentication authentication, @PathVariable String noteId) {
		String userId = getUserIdFromAuthentication(authentication);
		log.info("Request to get note: {} for user: {}", noteId, userId);

		try {
			UserNote note = userNoteService.getNoteById(userId, noteId);
			return ResponseEntity.ok(UserNoteDto.fromModel(note));
		} catch (IllegalArgumentException e) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
		}
	}

	@PatchMapping("/{noteId}")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<UserNoteDto> updateNote(Authentication authentication, @PathVariable String noteId,
			@Valid @RequestBody UpdateUserNoteRequest request) {
		String userId = getUserIdFromAuthentication(authentication);
		log.info("Request to update note: {} for user: {}", noteId, userId);

		try {
			UserNote note = userNoteService.updateNote(userId, noteId, request);
			return ResponseEntity.ok(UserNoteDto.fromModel(note));
		} catch (IllegalArgumentException e) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
		}
	}

	@DeleteMapping("/{noteId}")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<Void> deleteNote(Authentication authentication, @PathVariable String noteId) {
		String userId = getUserIdFromAuthentication(authentication);
		log.info("Request to delete note: {} for user: {}", noteId, userId);

		try {
			userNoteService.deleteNote(userId, noteId);
			return ResponseEntity.noContent().build();
		} catch (IllegalArgumentException e) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
		}
	}

	private String getUserIdFromAuthentication(Authentication authentication) {
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required.");
		}

		if (authentication.getPrincipal() instanceof Jwt jwt) {
			String userId = jwt.getSubject();
			if (userId == null || userId.isBlank()) {
				throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "User ID not found in token.");
			}
			return userId;
		} else {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
					"Cannot determine user ID from authentication principal.");
		}
	}
}
