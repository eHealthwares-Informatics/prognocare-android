package com.ehealthinformatics.prognocare.feature.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ehealthinformatics.prognocare.data.auth.SessionStore
import com.ehealthinformatics.prognocare.data.remote.AuthInterceptor
import com.ehealthinformatics.prognocare.data.remote.RetrofitClient
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

                val auth = loginResp.body()!!
                AuthInterceptor.saveToken(context, auth.accessToken)

                val meResp = authApi.me()
                if (!meResp.isSuccessful || meResp.body() == null) {
                    _state.value = LoginState.Error(
                        "Could not load profile: ${meResp.code()} ${meResp.message()}",
                    )
                    return@launch
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
            } catch (e: Exception) {
                _state.value = LoginState.Error("Network error: ${e.message}")
            }
        }
    }

    /**
     * Google social sign-in: the Credential Manager ID token is verified
     * server-side by the EMR (identity, channel='emr'); any existing account
     * role is linked by the provider-verified email, otherwise a patient
     * account is auto-registered. Session bootstrapping mirrors [login].
     */
    fun loginWithGoogle(idToken: String) {
        if (idToken.isBlank()) {
            _state.value = LoginState.Error("Google sign-in returned no token")
            return
        }
        if (_state.value is LoginState.Loading) return
        _state.value = LoginState.Loading
        viewModelScope.launch {
            try {
                val authApi = retrofitClient.apis.value.authApi
                val resp = authApi.googleSignIn(googleAuthRequest(idToken))
                if (!resp.isSuccessful || resp.body() == null) {
                    _state.value = LoginState.Error(
                        "Google sign-in failed: ${resp.code()} ${resp.message()}",
                    )
                    return@launch
                }
                AuthInterceptor.saveToken(context, resp.body()!!.accessToken)
                finishSessionBootstrap()
            } catch (e: Exception) {
                _state.value = LoginState.Error("Network error: ${e.message}")
            }
        }
    }

    /** Facebook social sign-in (verified server-side; see [loginWithGoogle]). */
    fun loginWithFacebook(accessToken: String) {
        if (accessToken.isBlank()) {
            _state.value = LoginState.Error("Facebook sign-in returned no token")
            return
        }
        if (_state.value is LoginState.Loading) return
        _state.value = LoginState.Loading
        viewModelScope.launch {
            try {
                val authApi = retrofitClient.apis.value.authApi
                val resp = authApi.facebookSignIn(facebookAuthRequest(accessToken))
                if (!resp.isSuccessful || resp.body() == null) {
                    _state.value = LoginState.Error(
                        "Facebook sign-in failed: ${resp.code()} ${resp.message()}",
                    )
                    return@launch
                }
                AuthInterceptor.saveToken(context, resp.body()!!.accessToken)
                finishSessionBootstrap()
            } catch (e: Exception) {
                _state.value = LoginState.Error("Network error: ${e.message}")
            }
        }
    }

    /** Shared post-token bootstrap: /auth/me → roles, staff linkage, splash state. */
    private suspend fun finishSessionBootstrap() {
        val authApi = retrofitClient.apis.value.authApi
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
        resolveStaff(me.id)
        _state.value = LoginState.Success(role)
    }

    /** Build the Google request DTO from the credential ID token. */
    private fun googleAuthRequest(idToken: String) =
        com.ehealthinformatics.prognocare.data.remote.models.GoogleAuthRequest(idToken = idToken)

    /** Build the Facebook request DTO from the LoginManager access token. */
    private fun facebookAuthRequest(accessToken: String) =
        com.ehealthinformatics.prognocare.data.remote.models.FacebookAuthRequest(accessToken = accessToken)

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
