package com.ehealthinformatics.prognocare.feature.dashboard.finance

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ehealthinformatics.prognocare.data.auth.SessionStore
import com.ehealthinformatics.prognocare.feature.records.EmrRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

/**
 * Finance dashboard. The EMR backend has no billing module, so revenue and
 * bills remain demo data (tagged in the UI); patient volume comes from the
 * live dashboard endpoint.
 */
@HiltViewModel
class FinanceDashboardViewModel @Inject constructor(
    private val emrRepository: EmrRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _state = MutableStateFlow(FinanceDashboardState())
    val state: StateFlow<FinanceDashboardState> = _state.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error

    init {
        loadDashboardData()
    }

    fun retry() = loadDashboardData()

    private fun loadDashboardData() {
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(isLoading = true, error = null)
                val today = java.time.LocalDate.now().toString()
                val summary = emrRepository.dashboard(today)
                _state.value = FinanceDashboardState(
                    greeting = greeting(),
                    financeName = SessionStore.getStaffName(context) ?: "Finance",
                    todayDate = todayDate(),
                    totalRevenue = "₦0",
                    pendingBills = summary.metrics.pendingRequests,
                    completedPayments = summary.metrics.completed,
                    overdueBills = 0,
                    recentBills = emptyList(),
                    recentPayments = emptyList(),
                    isLoading = false,
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = e.message ?: "Failed to load")
            }
        }
    }

    private fun greeting(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when {
            hour < 12 -> "Good morning"
            hour < 17 -> "Good afternoon"
            else -> "Good evening"
        }
    }

    private fun todayDate(): String {
        val sdf = SimpleDateFormat("EEEE, MMM d", Locale.US)
        return sdf.format(Calendar.getInstance().time)
    }
}
