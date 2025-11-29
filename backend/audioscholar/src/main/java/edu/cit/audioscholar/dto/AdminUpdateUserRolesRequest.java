package edu.cit.audioscholar.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;

public record AdminUpdateUserRolesRequest(@NotEmpty(message = "Roles list cannot be empty") List<String> roles) {
}
