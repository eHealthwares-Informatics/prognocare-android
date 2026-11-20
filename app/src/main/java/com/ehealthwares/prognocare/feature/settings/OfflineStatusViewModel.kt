package com.ehealthwares.prognocare.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ehealthwares.prognocare.data.local.OfflineStore
import com.ehealthwares.prognocare.data.network.ConnectivityObserver
import com.ehealthwares.prognocare.data.sync.OfflineSyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Surfaces offline-mode status (pending writes, caches, sync) to Settings. */
@HiltViewModel
class OfflineStatusViewModel @Inject constructor(
    private val offlineStore: OfflineStore,
    private val syncManager: OfflineSyncManager,
    connectivity: ConnectivityObserver,
) : ViewModel() {

    val pendingCount: StateFlow<Int> = offlineStore.observePendingCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val cachedPatients: StateFlow<Int> = offlineStore.observeCachedPatientCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val isOnline: StateFlow<Boolean> = connectivity.isOnline

    val isSyncing: StateFlow<Boolean> = syncManager.isSyncing

    fun syncNow() {
        viewModelScope.launch {
            syncManager.refreshPatientCache()
            syncManager.syncNow()
        }
    }
}
