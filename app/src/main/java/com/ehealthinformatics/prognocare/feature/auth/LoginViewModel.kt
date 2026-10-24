package com.ehealthinformatics.prognocare.feature.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ehealthinformatics.prognocare.data.auth.SessionStore
import com.ehealthinformatics.prognocare.data.remote.AuthInterceptor
import com.ehealthinformatics.prognocare.data.remote.RetrofitClient
import com.ehealthinformatics.prognocare.data.remote.api.AuthApi
import com.ehealthinformatics.prognocare.data.remote.models.FacebookAuthRequest
import com.ehealthinformatics.prognocare.data.remote.models.GoogleAuthRequest
import com.ehealthinformatics.prognocare.data.remote.models.LoginDto
import com.ehealthinformatics.prognocare.feature.splash.SplashViewModel
import com.ehealthinformatics.prognocare.navigation.UserRole
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class LoginState {
    data object Idle : LoginState()
    data object Loading : LoginState()
    data class Success(val role: UserRole) : LoginState()
    data class Error(val message: String) : LoginState()
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val retrofitClient: RetrofitClient,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _state = MutableStateFlow<LoginState>(LoginState.Idle)
    val state: StateFlow<LoginState> = _state.asStateFlow()

    fun login(username: String, password: String) {
        if (_state.value is LoginState.Loading) return
        _state.value = LoginState.Loading

        viewModelScope.launch {
            try {
                val authApi = retrofitClient.apis.value.authApi

                val loginResp = authApi.login(LoginDto(username = username, password = password))
                if (!loginResp.isSuccessful || loginResp.body() == null) {
                    _state.value = LoginState.Error(
                        "Login failed: ${loginResp.code()} ${loginResp.message()}",
                    )
                    return@launch
                }

                finishSignIn(authApi, loginResp.body()!!.accessToken)
            } catch (e: Exception) {
                _state.value = LoginState.Error("Network error: ${e.message}")
            }
        }
    }

    /**
     * Google OAuth sign-in. The ID token from Credential Manager is exchanged
     * for the shared identity JWT by the EMR proxy, which creates/links the
     * user and captures the Google profile fields server-side.
     */
    fun signInWithGoogle(idToken: String) {
        if (_state.value is LoginState.Loading) return
        _state.value = LoginState.Loading

        viewModelScope.launch {
            try {
                val authApi = retrofitClient.apis.value.authApi
                val resp = authApi.googleSignIn(GoogleAuthRequest(idToken = idToken))
                if (!resp.isSuccessful || resp.body() == null) {
                    _state.value = LoginState.Error(
                        "Google sign-in failed: ${resp.code()} ${resp.message()}",
                    )
                    return@launch
                }
                finishSignIn(authApi, resp.body()!!.accessToken)
            } catch (e: Exception) {
                _state.value = LoginState.Error("Network error: ${e.message}")
            }
        }
    }

    /**
     * Facebook OAuth sign-in. The SDK access token is exchanged for the shared
     * identity JWT; the backend verifies it with the app secret (never shipped
     * in the client) and captures the Facebook profile fields.
     */
    fun signInWithFacebook(accessToken: String) {
        if (_state.value is LoginState.Loading) return
        _state.value = LoginState.Loading

        viewModelScope.launch {
            try {
                val authApi = retrofitClient.apis.value.authApi
                val resp = authApi.facebookSignIn(FacebookAuthRequest(accessToken = accessToken))
                if (!resp.isSuccessful || resp.body() == null) {
                    _state.value = LoginState.Error(
                        "Facebook sign-in failed: ${resp.code()} ${resp.message()}",
                    )
                    return@launch
                }
                finishSignIn(authApi, resp.body()!!.accessToken)
            } catch (e: Exception) {
                _state.value = LoginState.Error("Network error: ${e.message}")
            }
        }
    }

    /** Stores the token, loads the profile and resolves the staff record. */
    private suspend fun finishSignIn(authApi: AuthApi, accessToken: String) {
        AuthInterceptor.saveToken(context, accessToken)

        val meResp = authApi.me()
        if (!meResp.isSuccessful || meResp.body() == null) {
            _state.value = LoginState.Error(
                "Could not load profile: ${meResp.code()} ${meResp.message()}",
            )
            return
        }

        val me = meResp.body()!!
        SessionStore.saveUserId(context, me.id)
        val role = UserRoleMapper.map(me)
        SplashViewModel.saveAuthState(context, role)
        SessionStore.saveRole(context, role)

        // Best-effort: resolve the staff record linked to this identity
        // user so clinician dashboards can self-scope their queries.
        resolveStaff(me.id)

        _state.value = LoginState.Success(role)
    }

    private suspend fun resolveStaff(userId: String) {
        runCatching {
            val response = retrofitClient.apis.value.staffApi.list(userId = userId, limit = 1)
            val staff = response.body()?.data?.firstOrNull()
            if (staff != null) {
                SessionStore.saveStaff(context, staff.id, staff.displayName)
                // Seed the location scope with the staff record's location so
                // list queries are location-based from the first launch.
                staff.identityLocationId?.let { SessionStore.saveStaffLocationId(context, it) }
                // Seed the chat identity with the staff phone so messaging
                // routes to a stable participant (replies arrive per-phone).
                staff.phone?.takeIf { it.isNotBlank() }?.let {
                    com.ehealthinformatics.prognocare.feature.chat.ChatIdentity.savePhone(context, it)
                }
            }
        }
    }

    fun reset() {
        _state.value = LoginState.Idle
    }
}
