package com.ehealthinformatics.prognocare.feature.clinical

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ehealthinformatics.prognocare.data.remote.RetrofitClient
import com.ehealthinformatics.prognocare.data.remote.models.Admission
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AdmissionsListState(
    val isLoading: Boolean = true,
    val admissions: List<Admission> = emptyList(),
    val error: String? = null,
)

/** Admissions list (active + discharged). */
@HiltViewModel
class AdmissionsListViewModel @Inject constructor(
    private val retrofitClient: RetrofitClient,
) : ViewModel() {

    private val _state = MutableStateFlow(AdmissionsListState())
    val state: StateFlow<AdmissionsListState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val response = retrofitClient.apis.value.admissionApi.list(limit = 100)
                if (response.isSuccessful) {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        admissions = response.body()?.data.orEmpty(),
                    )
                } else {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "${response.code()} ${response.message()}",
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load admissions",
                )
            }
        }
    }

    fun retry() = load()
}
