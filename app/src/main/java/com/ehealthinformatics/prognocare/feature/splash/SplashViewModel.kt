package com.ehealthinformatics.prognocare.feature.splash

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ehealthinformatics.prognocare.navigation.UserRole
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SplashDestination {
    data object Login : SplashDestination()
    data class Dashboard(val role: UserRole) : SplashDestination()
}

@HiltViewModel
class SplashViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _destination = MutableSharedFlow<SplashDestination>(extraBufferCapacity = 1)
    val destination: SharedFlow<SplashDestination> = _destination.asSharedFlow()

    init {
        checkAuthState()
    }

    private fun checkAuthState() {
        viewModelScope.launch {
            // Show splash for minimum duration (branding)
            delay(2000L)

            val prefs = context.getSharedPreferences("prognocare_auth", Context.MODE_PRIVATE)
            val isLoggedIn = prefs.getBoolean("is_logged_in", false)
            val roleOrdinal = prefs.getInt("user_role", -1)

            if (isLoggedIn && roleOrdinal in UserRole.entries.indices) {
                _destination.emit(SplashDestination.Dashboard(UserRole.entries[roleOrdinal]))
            } else {
                _destination.emit(SplashDestination.Login)
            }
        }
    }

    companion object {
        fun saveAuthState(context: Context, role: UserRole) {
            context.getSharedPreferences("prognocare_auth", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("is_logged_in", true)
                .putInt("user_role", role.ordinal)
                .apply()
        }

        /** The role persisted by [saveAuthState], or null when not signed in. */
        fun loadRole(context: Context): UserRole? {
            val ordinal = context.getSharedPreferences("prognocare_auth", Context.MODE_PRIVATE)
                .getInt("user_role", -1)
            return UserRole.entries.getOrNull(ordinal)
        }

        fun clearAuthState(context: Context) {
            context.getSharedPreferences("prognocare_auth", Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply()
        }
    }
}
