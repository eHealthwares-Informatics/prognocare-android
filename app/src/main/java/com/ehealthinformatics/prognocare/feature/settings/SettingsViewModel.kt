package com.ehealthinformatics.prognocare.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ehealthinformatics.prognocare.data.config.AppConfig
import com.ehealthinformatics.prognocare.data.config.AppConfigStore
import com.ehealthinformatics.prognocare.data.config.ConnectionCheck
import com.ehealthinformatics.prognocare.data.config.ServerConfigVerifier
import com.ehealthinformatics.prognocare.data.config.ServerDiscovery
import com.ehealthinformatics.prognocare.data.config.withConversationBaseUrl
import com.ehealthinformatics.prognocare.data.config.withEmrBaseUrl
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

    fun resetToDefaults() {
        viewModelScope.launch {
            _isVerifying.value = true
            _saveResult.value = null
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