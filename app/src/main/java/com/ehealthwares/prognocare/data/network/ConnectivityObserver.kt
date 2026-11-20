package com.ehealthwares.prognocare.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** App-wide online/offline signal driven by the default network callback. */
@Singleton
class ConnectivityObserver @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val _isOnline = MutableStateFlow(context.hasInternet())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    init {
        val manager = context.getSystemService(ConnectivityManager::class.java)
        runCatching {
            manager?.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    _isOnline.value = true
                }

                override fun onLost(network: Network) {
                    // Another network may still be up; re-evaluate.
                    _isOnline.value = context.hasInternet()
                }

                override fun onCapabilitiesChanged(
                    network: Network,
                    networkCapabilities: NetworkCapabilities,
                ) {
                    _isOnline.value = networkCapabilities
                        .hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                }

                override fun onUnavailable() {
                    _isOnline.value = false
                }
            })
        }
    }

    val online: Boolean get() = _isOnline.value
}

private fun Context.hasInternet(): Boolean {
    val manager = getSystemService(ConnectivityManager::class.java) ?: return false
    val network = manager.activeNetwork ?: return false
    val capabilities = manager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}
