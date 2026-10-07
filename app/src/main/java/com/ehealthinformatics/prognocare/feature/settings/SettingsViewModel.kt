package com.ehealthinformatics.prognocare.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ehealthinformatics.prognocare.data.config.AppConfig
import com.ehealthinformatics.prognocare.data.config.AppConfigStore
import com.ehealthinformatics.prognocare.data.config.ConnectionCheck
import com.ehealthinformatics.prognocare.data.config.ServerConfigVerifier
import com.ehealthinformatics.prognocare.data.config.withConversationBaseUrl
import com.ehealthinformatics.prognocare.data.config.withEmrBaseUrl
import com.ehealthinformatics.prognocare.data.config.withQueryDateRange
import com.ehealthinformatics.prognocare.data.config.withWebChannelCode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SaveResult(
    val saved: Boolean,
    val checks: List<ConnectionCheck> = emptyList(),
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val configStore: AppConfigStore,
    private val verifier: ServerConfigVerifier,
) : ViewModel() {

    val config: StateFlow<AppConfig> = configStore.config
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), configStore.config.value)

    private val _isVerifying = MutableStateFlow(false)
    val isVerifying: StateFlow<Boolean> = _isVerifying.asStateFlow()

    private val _saveResult = MutableStateFlow<SaveResult?>(null)
    val saveResult: StateFlow<SaveResult?> = _saveResult.asStateFlow()

    private val _dateRangeSaving = MutableStateFlow(false)
    val dateRangeSaving: StateFlow<Boolean> = _dateRangeSaving.asStateFlow()

    private val _dateRangeMessage = MutableStateFlow<String?>(null)
    val dateRangeMessage: StateFlow<String?> = _dateRangeMessage.asStateFlow()

    fun saveConfig(
        emrBaseUrl: String,
        conversationBaseUrl: String,
        webChannelCode: String,
    ) {
        viewModelScope.launch {
            _isVerifying.value = true
            _saveResult.value = null
            try {
                val current = configStore.config.value
                val candidate = current
                    .withEmrBaseUrl(emrBaseUrl)
                    .withConversationBaseUrl(conversationBaseUrl)
                    .withWebChannelCode(webChannelCode)

                val checks = verifier.verify(candidate)
                val allOk = checks.all { it is ConnectionCheck.Success }
                if (allOk) {
                    configStore.updateConfig(candidate)
                }
                _saveResult.value = SaveResult(saved = allOk, checks = checks)
            } finally {
                _isVerifying.value = false
            }
        }
    }

    /**
     * Saves the global query date-range used by appointment/dashboard queries.
     * Pass nulls to clear (queries fall back to today).
     */
    fun saveQueryDateRange(start: String?, end: String?) {
        viewModelScope.launch {
            _dateRangeSaving.value = true
            _dateRangeMessage.value = null
            try {
                val current = configStore.config.value
                configStore.updateConfig(current.withQueryDateRange(start, end))
                _dateRangeMessage.value = when {
                    start == null && end == null ->
                        "Query date range cleared — queries use today."
                    else ->
                        "Query date range saved — appointment queries use this window."
                }
            } catch (e: Exception) {
                _dateRangeMessage.value = e.message ?: "Could not save date range"
            } finally {
                _dateRangeSaving.value = false
            }
        }
    }

    fun clearQueryDateRange() = saveQueryDateRange(null, null)

    fun dismissDateRangeMessage() {
        _dateRangeMessage.value = null
    }

    fun resetToDefaults() {
        viewModelScope.launch {
            _isVerifying.value = true
            _saveResult.value = null
            _dateRangeMessage.value = null
            try {
                configStore.resetToDefaults()
                _saveResult.value = SaveResult(saved = true)
            } finally {
                _isVerifying.value = false
            }
        }
    }

    fun dismissResult() {
        _saveResult.value = null
    }
}