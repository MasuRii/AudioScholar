package edu.cit.audioscholar.data.remote.dto.admin

import com.google.gson.annotations.SerializedName

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
    @SerializedName("roles") val rolesList: List<String>? = null
) {
    val roles: List<String>
        get() = rolesList ?: emptyList()

    val isAdmin: Boolean get() = roles.contains("ROLE_ADMIN")
    val isPremium: Boolean get() = roles.contains("ROLE_PREMIUM")
}

data class AdminUserListResponse(
    val users: List<AdminUserDto>,
    val pageToken: String?
)