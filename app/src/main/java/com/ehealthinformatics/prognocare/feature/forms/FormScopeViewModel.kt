package com.ehealthinformatics.prognocare.feature.forms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ehealthinformatics.prognocare.data.remote.RetrofitClient
import com.ehealthinformatics.prognocare.data.remote.models.Encounter
import com.ehealthinformatics.prognocare.data.remote.models.Patient
import com.ehealthinformatics.prognocare.data.remote.models.Visit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FormScopeState(
    val patientQuery: String = "",
    val patientResults: List<Patient> = emptyList(),
    val visits: List<Visit> = emptyList(),
    val encounters: List<Encounter> = emptyList(),
    val isLoading: Boolean = false,
)

/**
 * Loads the patient / visit / encounter scope for documentation entry — the
 * same semantics as the New Request screen (patients without an open visit or
 * active encounter, ongoing visits, active encounters).
 */
@HiltViewModel
class FormScopeViewModel @Inject constructor(
    private val retrofitClient: RetrofitClient,
) : ViewModel() {

    private val _state = MutableStateFlow(FormScopeState())
    val state: StateFlow<FormScopeState> = _state.asStateFlow()

    init {
        loadScope()
    }

    fun refresh() = loadScope()

    private fun loadScope() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            try {
                val apis = retrofitClient.apis.value
                val visits = runCatching {
                    apis.visitApi.list(status = "ONGOING", limit = 100).body()?.data.orEmpty()
                }.getOrDefault(emptyList())
                val encounters = runCatching {
                    apis.encounterApi.list(limit = 100).body()?.data.orEmpty().filter { it.isActive }
                }.getOrDefault(emptyList())
                _state.value = _state.value.copy(
                    visits = visits.distinctBy { it.patientId },
                    encounters = encounters.distinctBy { it.patientId },
                    isLoading = false,
                )
            } catch (_: Exception) {
                _state.value = _state.value.copy(isLoading = false)
            }
        }
    }

    fun searchPatients(query: String) {
        if (query.isBlank()) {
            _state.value = _state.value.copy(patientResults = emptyList())
            return
        }
        viewModelScope.launch {
            try {
                val resp = retrofitClient.apis.value.patientApi.list(page = 1, limit = 10, search = query)
                _state.value = _state.value.copy(
                    patientResults = if (resp.isSuccessful) resp.body()?.data.orEmpty() else emptyList(),
                )
            } catch (_: Exception) {
                _state.value = _state.value.copy(patientResults = emptyList())
            }
        }
    }
}
