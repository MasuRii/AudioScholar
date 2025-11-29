package edu.cit.audioscholar.data.remote.dto.admin

data class AdminUpdateUserStatusRequest(
    val disabled: Boolean
)

data class AdminUpdateUserRolesRequest(
    val roles: List<String>
)

data class AdminUserDto(
    val uid: String,
    val email: String?,
    val displayName: String?,
    val photoUrl: String?,
    val disabled: Boolean,
    val emailVerified: Boolean,
    val customClaims: Map<String, Any>?
)

data class AdminUserListResponse(
    val users: List<AdminUserDto>,
    val pageToken: String?
)