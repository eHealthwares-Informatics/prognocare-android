package com.ehealthinformatics.prognocare.feature.dashboard.nurse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ehealthinformatics.prognocare.data.remote.RetrofitClient
import com.ehealthinformatics.prognocare.data.remote.models.FormSubmission
import com.ehealthinformatics.prognocare.feature.forms.VitalsReadings
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class VitalsListItem(
    val submission: FormSubmission,
    val readings: VitalsReadings.Vitals,
)

data class VitalsListState(
    val items: List<VitalsListItem> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
)

/**
 * Loads VITALS form submissions for the list screen. Entries are filtered
 * client-side by form name (contains "vital") because EMR list filters are
 * by formDefinitionId, not code.
 */
@HiltViewModel
class VitalsListViewModel @Inject constructor(
    private val retrofitClient: RetrofitClient,
) : ViewModel() {

    private val _state = MutableStateFlow(VitalsListState())
    val state: StateFlow<VitalsListState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = _state.value.items.isEmpty(), isRefreshing = true, error = null)
            try {
                val response = retrofitClient.apis.value.formApi.listSubmissions(limit = 100)
                val all = if (response.isSuccessful) response.body()?.data.orEmpty() else emptyList()
                val vitals = all
                    .filter { it.formName.contains("vital", ignoreCase = true) }
                    .filter { it.status != "DRAFT" }
                    .sortedByDescending { it.submittedAt ?: it.createdAt.orEmpty() }
                    .mapNotNull { submission ->
                        VitalsReadings.from(submission)?.let { VitalsListItem(submission, it) }
                    }
                _state.value = VitalsListState(
                    items = vitals,
                    isLoading = false,
                    isRefreshing = false,
                    error = if (!response.isSuccessful) {
                        "${response.code()} ${response.message()}"
                    } else {
                        null
                    },
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    error = e.message ?: "Failed to load vitals",
                )
            }
        }
    }

    fun retry() {
        _state.value = _state.value.copy(error = null)
        load()
    }
}
