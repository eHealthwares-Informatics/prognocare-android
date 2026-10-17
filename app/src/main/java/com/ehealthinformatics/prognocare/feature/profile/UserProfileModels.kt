package com.ehealthinformatics.prognocare.feature.profile

import com.ehealthinformatics.prognocare.navigation.UserRole

data class UserProfile(
    val id: String,
    val name: String,
    val email: String,
    val phone: String,
    val role: UserRole,
    val department: String,
    val facility: String,
    val avatarUrl: String? = null,
    val employeeId: String? = null,
    val joinDate: String? = null,
    val licenseNumber: String? = null,
    val specialty: String? = null,
)

data class UserProfileState(
    /**
     * True only for the brief local seed on first frame. Network refresh uses
     * [isRefreshing] so the screen is interactive immediately.
     */
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val profile: UserProfile? = null,
    val isSigningOut: Boolean = false,
)

sealed class UserProfileEvent {
    data object SignOut : UserProfileEvent()
    data class UpdateProfile(val profile: UserProfile) : UserProfileEvent()
}
