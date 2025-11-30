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
    val customClaims: Map<String, Any>?,
    @SerializedName("roles") val rolesList: List<String>? = null,
    @SerializedName("role") val roleSingle: String? = null
) {
    val roles: List<String>
        get() {
            val allRoles = mutableSetOf<String>()

            // 1. Try to get from top-level fields
            rolesList?.let { allRoles.addAll(it) }
            roleSingle?.let { allRoles.add(it) }

            // 2. Try to get from customClaims
            customClaims?.let { claims ->
                try {
                    // Check 'roles' key
                    val rolesClaim = claims["roles"]
                    when (rolesClaim) {
                        is List<*> -> allRoles.addAll(rolesClaim.filterIsInstance<String>())
                        is String -> allRoles.add(rolesClaim)
                    }

                    // Check 'role' key (singular)
                    val roleClaim = claims["role"]
                    if (roleClaim is String) {
                        allRoles.add(roleClaim)
                    }
                } catch (e: Exception) {
                    // Ignore parsing errors
                }
            }

            return allRoles.toList()
        }

    val isAdmin: Boolean get() = roles.contains("ROLE_ADMIN")
    val isPremium: Boolean get() = roles.contains("ROLE_PREMIUM")
}

data class AdminUserListResponse(
    val users: List<AdminUserDto>,
    val pageToken: String?
)