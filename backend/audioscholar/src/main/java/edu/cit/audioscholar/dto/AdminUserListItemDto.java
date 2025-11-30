package edu.cit.audioscholar.dto;

import java.util.List;

public record AdminUserListItemDto(String uid, String email, String displayName, String photoUrl, boolean disabled,
		boolean emailVerified, List<String> roles) {
}
