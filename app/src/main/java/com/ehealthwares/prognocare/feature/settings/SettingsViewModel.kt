package com.ehealthwares.prognocare.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ehealthwares.prognocare.data.config.AppConfig
import com.ehealthwares.prognocare.data.config.AppConfigStore
import com.ehealthwares.prognocare.data.config.ConnectionCheck
import com.ehealthwares.prognocare.data.config.ServerConfigVerifier
import com.ehealthwares.prognocare.data.config.ServerDiscovery
import com.ehealthwares.prognocare.data.config.QueryRangePeriod
import com.ehealthwares.prognocare.data.config.ServerEnvironment
import com.ehealthwares.prognocare.data.config.withConversationBaseUrl
import com.ehealthwares.prognocare.data.config.withEmrBaseUrl
import com.ehealthwares.prognocare.data.config.withQueryRange
import com.ehealthwares.prognocare.data.config.withServerEnvironment
import com.ehealthwares.prognocare.data.config.withWebChannelCode
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

/** Result of a local-network sweep for reachable EMR servers. */
data class DiscoveryState(
    val scanning: Boolean = false,
    val servers: List<String> = emptyList(),
    val message: String? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val configStore: AppConfigStore,
    private val verifier: ServerConfigVerifier,
    private val serverDiscovery: ServerDiscovery,
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
    private val _discovery = MutableStateFlow(DiscoveryState())
    val discovery: StateFlow<DiscoveryState> = _discovery.asStateFlow()

    /** Sweeps the reachable LAN (Wi-Fi or the phone's hotspot) for an EMR server. */
    fun discoverServers() {
        if (_discovery.value.scanning) return
        _discovery.value = DiscoveryState(scanning = true)
        viewModelScope.launch {
            val result = runCatching { serverDiscovery.scan() }
            _discovery.value = result.fold(
                onSuccess = { scan ->
                    if (scan.servers.isNotEmpty()) {
                        DiscoveryState(servers = scan.servers.map { it.url })
                    } else {
                        DiscoveryState(
                            message = if (scan.subnets.isEmpty()) {
                                "Not on Wi-Fi or a hotspot — no local network to scan."
                            } else {
                                "No server on ${scan.subnets.joinToString { "$it.x" }}."
                            },
                        )
                    }
                },
                onFailure = { DiscoveryState(message = it.message ?: "Scan failed") },
            )
        }
    }

    fun dismissDiscovery() {
        _discovery.value = DiscoveryState()
    }

    fun saveConfig(
        emrBaseUrl: String,
        conversationBaseUrl: String,
        webChannelCode: String,
        environment: ServerEnvironment? = null,
    ) {
        viewModelScope.launch {
            _isVerifying.value = true
            _saveResult.value = null
            try {
                val current = configStore.config.value
                val env = environment ?: current.serverEnvironment
                val candidate = current
                    .withServerEnvironment(env)
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

    /** Saves the relative query range period (dates resolved at query time). */
    fun saveQueryRange(period: QueryRangePeriod) {
        viewModelScope.launch {
            _dateRangeSaving.value = true
            _dateRangeMessage.value = null
            try {
                val current = configStore.config.value
                configStore.updateConfig(current.withQueryRange(period))
                _dateRangeMessage.value = when (period) {
                    QueryRangePeriod.NONE -> "Query range cleared — no filter (dashboards use today)."
                    else -> "Query range saved — queries use “${period.label}”."
                }
            } catch (e: Exception) {
                _dateRangeMessage.value = e.message ?: "Could not save query range"
            } finally {
                _dateRangeSaving.value = false
            }
        }
    }

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