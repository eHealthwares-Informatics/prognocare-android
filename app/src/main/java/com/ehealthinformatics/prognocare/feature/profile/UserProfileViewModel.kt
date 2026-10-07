package com.ehealthinformatics.prognocare.feature.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ehealthinformatics.prognocare.data.auth.SessionStore
import com.ehealthinformatics.prognocare.data.config.AppConfig
import com.ehealthinformatics.prognocare.data.config.AppConfigStore
import com.ehealthinformatics.prognocare.data.remote.RetrofitClient
import com.ehealthinformatics.prognocare.data.remote.models.MeResponse
import com.ehealthinformatics.prognocare.feature.splash.SplashViewModel
import com.ehealthinformatics.prognocare.navigation.UserRole
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserProfileViewModel @Inject constructor(
    private val retrofitClient: RetrofitClient,
    private val configStore: AppConfigStore,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _state = MutableStateFlow(UserProfileState())
    val state: StateFlow<UserProfileState> = _state.asStateFlow()

    private val _signOutComplete = MutableStateFlow(false)
    val signOutComplete: StateFlow<Boolean> = _signOutComplete.asStateFlow()

    private val _roleSwitchedTo = MutableStateFlow<UserRole?>(null)
    val roleSwitchedTo: StateFlow<UserRole?> = _roleSwitchedTo.asStateFlow()

    val appConfig: StateFlow<AppConfig> = configStore.config
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), configStore.config.value)

    init {
        // Show the profile screen immediately from local session + role seed;
        // network-dependent fields refresh in the background without blocking.
        val role = SessionStore.getRole(context)
            ?: SplashViewModel.loadRole(context)
            ?: UserRole.Doctor
        _state.value = UserProfileState(
            isLoading = false,
            isRefreshing = true,
            profile = localProfile(role),
        )
        refreshProfile(role)
    }

    private fun refreshProfile(role: UserRole) {
        viewModelScope.launch {
            val profile = loadProfileFromApi(role)
            _state.update {
                it.copy(isRefreshing = false, profile = profile ?: it.profile)
            }
        }
    }

    /**
     * Loads the signed-in identity via GET /api/auth/me plus the linked staff
     * record (GET /api/staff?userId=…) when one exists. Falls back to the
     * role-based local profile when the backend is unreachable.
     */
    private suspend fun loadProfileFromApi(role: UserRole): UserProfile? {
        val me = runCatching { fetchMe() }.getOrNull()
        val staff = me?.let { runCatching { fetchMyStaff(it.id) }.getOrNull() }
        if (me == null && staff == null) return null
        return UserProfile(
            id = staff?.id ?: me?.id.orEmpty(),
            name = staff?.let { "${it.firstName} ${it.lastName}".trim() }
                ?: me?.username.orEmpty().ifBlank { role.displayName },
            email = staff?.email ?: me?.username.orEmpty(),
            phone = staff?.phone.orEmpty(),
            role = role,
            department = staff?.department ?: role.displayName,
            facility = SessionStore.getStaffLocation(context).orEmpty().ifBlank { "PrognoCare" },
            employeeId = staff?.staffNumber,
            joinDate = staff?.hireDate,
        )
    }

    /** Instant first-frame profile from session + role (no network). */
    private fun localProfile(role: UserRole): UserProfile {
        val fallback = getMockProfile(role)
        val name = SessionStore.getStaffName(context) ?: fallback.name
        return UserProfile(
            id = SessionStore.getStaffId(context) ?: SessionStore.getUserId(context) ?: fallback.id,
            name = name,
            email = fallback.email,
            phone = fallback.phone,
            role = role,
            department = fallback.department,
            facility = SessionStore.getStaffLocation(context)
                .orEmpty().ifBlank { fallback.facility },
            employeeId = fallback.employeeId,
            joinDate = fallback.joinDate,
        )
    }

    private suspend fun fetchMe(): MeResponse {
        val response = retrofitClient.apis.value.authApi.me()
        if (!response.isSuccessful) throw com.ehealthinformatics.prognocare.feature.appointments.ApiException(response.code(), "me failed")
        return response.body() ?: throw com.ehealthinformatics.prognocare.feature.appointments.ApiException(response.code(), "empty me")
    }

    private suspend fun fetchMyStaff(userId: String): com.ehealthinformatics.prognocare.data.remote.models.Staff? {
        val response = retrofitClient.apis.value.staffApi.list(userId = userId, limit = 1)
        if (!response.isSuccessful) return null
        return response.body()?.data?.firstOrNull()
    }

    fun saveServerConfig(emr: String, conversation: String, webChannelCode: String) {
        viewModelScope.launch {
            configStore.updateConfig(
                configStore.config.value.copy(
                    emrBaseUrl = emr,
                    conversationBaseUrl = conversation,
                    webChannelCode = webChannelCode,
                ),
            )
        }
    }

    fun resetServerConfig() {
        viewModelScope.launch { configStore.resetToDefaults() }
    }

    /**
     * Demo/dev affordance: switch the active role. Persists the new role the
     * same way login does ([SplashViewModel.saveAuthState] + [SessionStore])
     * so the splash gate and all role-scoped queries pick it up, then signals
     * the UI to navigate to the new role's dashboard.
     */
    fun switchRole(role: UserRole) {
        val current = _state.value.profile?.role
        if (role == current || _state.value.isSigningOut) return
        SplashViewModel.saveAuthState(context, role)
        SessionStore.saveRole(context, role)
        _state.update { it.copy(profile = it.profile?.copy(role = role)) }
        _roleSwitchedTo.value = role
    }

    fun consumeRoleSwitch() {
        _roleSwitchedTo.value = null
    }

    fun signOut() {
        viewModelScope.launch {
            _state.update { it.copy(isSigningOut = true) }
            SplashViewModel.clearAuthState(context)
            _signOutComplete.value = true
        }
    }

    private fun getMockProfile(role: UserRole): UserProfile {
        return when (role) {
            UserRole.Doctor -> UserProfile(
                id = "DOC-001",
                name = "Dr. Chidi Okonkwo",
                email = "chidi.okonkwo@prognocare.com",
                phone = "+234 801 234 5678",
                role = UserRole.Doctor,
                department = "Internal Medicine",
                facility = "PrognoCare General Hospital",
                employeeId = "EMP-2024-001",
                joinDate = "Jan 15, 2022",
                licenseNumber = "MDCN/2021/12345",
                specialty = "Cardiology",
            )
            UserRole.Nurse -> UserProfile(
                id = "NRS-001",
                name = "Nurse Amara Eze",
                email = "amara.eze@prognocare.com",
                phone = "+234 802 345 6789",
                role = UserRole.Nurse,
                department = "Emergency Unit",
                facility = "PrognoCare General Hospital",
                employeeId = "EMP-2024-002",
                joinDate = "Mar 20, 2023",
                licenseNumber = "NDC/2022/67890",
            )
            UserRole.Patient -> UserProfile(
                id = "PAT-001",
                name = "Chidi Okonkwo",
                email = "chidi.okonkwo@email.com",
                phone = "+234 803 456 7890",
                role = UserRole.Patient,
                department = "Patient",
                facility = "PrognoCare General Hospital",
                joinDate = "Aug 10, 2024",
            )
            UserRole.Specialist -> UserProfile(
                id = "SPE-001",
                name = "Dr. Fatima Bello",
                email = "fatima.bello@prognocare.com",
                phone = "+234 804 567 8901",
                role = UserRole.Specialist,
                department = "Cardiology",
                facility = "PrognoCare Specialist Center",
                employeeId = "EMP-2024-003",
                joinDate = "Jun 1, 2021",
                licenseNumber = "MDCN/2020/54321",
                specialty = "Interventional Cardiology",
            )
            UserRole.Therapist -> UserProfile(
                id = "THR-001",
                name = "Ibrahim Musa",
                email = "ibrahim.musa@prognocare.com",
                phone = "+234 805 678 9012",
                role = UserRole.Therapist,
                department = "Physical Therapy",
                facility = "PrognoCare Rehabilitation Center",
                employeeId = "EMP-2024-004",
                joinDate = "Sep 15, 2023",
                licenseNumber = "PCN/2022/98765",
                specialty = "Musculoskeletal Therapy",
            )
            UserRole.Technician -> UserProfile(
                id = "TEC-001",
                name = "Kemi Adeyemi",
                email = "kemi.adeyemi@prognocare.com",
                phone = "+234 806 789 0123",
                role = UserRole.Technician,
                department = "Laboratory",
                facility = "PrognoCare General Hospital",
                employeeId = "EMP-2024-005",
                joinDate = "Feb 10, 2024",
            )
            UserRole.Finance -> UserProfile(
                id = "FIN-001",
                name = "Aisha Abdullahi",
                email = "aisha.abdullahi@prognocare.com",
                phone = "+234 807 890 1234",
                role = UserRole.Finance,
                department = "Finance & Billing",
                facility = "PrognoCare General Hospital",
                employeeId = "EMP-2024-006",
                joinDate = "Apr 5, 2022",
            )
            UserRole.Support -> UserProfile(
                id = "SUP-001",
                name = "Emeka Nwosu",
                email = "emeka.nwosu@prognocare.com",
                phone = "+234 808 901 2345",
                role = UserRole.Support,
                department = "Patient Services",
                facility = "PrognoCare General Hospital",
                employeeId = "EMP-2024-007",
                joinDate = "Jul 20, 2023",
            )
            UserRole.Admin -> UserProfile(
                id = "ADM-001",
                name = "Oluwaseun Bakare",
                email = "oluwaseun.bakare@prognocare.com",
                phone = "+234 809 012 3456",
                role = UserRole.Admin,
                department = "Administration",
                facility = "PrognoCare General Hospital",
                employeeId = "EMP-2024-008",
                joinDate = "Jan 1, 2021",
            )
        }
    }
}
