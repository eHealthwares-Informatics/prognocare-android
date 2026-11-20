package com.ehealthwares.prognocare.data.sync

import com.ehealthwares.prognocare.data.local.OfflineStore
import com.ehealthwares.prognocare.data.local.PendingActionEntity
import com.ehealthwares.prognocare.data.local.PendingActionType
import com.ehealthwares.prognocare.data.network.ConnectivityObserver
import com.ehealthwares.prognocare.data.remote.RetrofitClient
import com.ehealthwares.prognocare.data.remote.models.CreateAppointmentDto
import com.ehealthwares.prognocare.data.remote.models.CreateFormSubmissionDto
import com.ehealthwares.prognocare.data.remote.models.CreatePatientDto
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

data class SyncResult(
    val synced: Int = 0,
    val failed: Int = 0,
    val offline: Boolean = false,
)

/**
 * Flushes the offline outbox and refreshes the on-device patient cache.
 *
 * Runs on app start and whenever connectivity returns. Writes captured while
 * offline are replayed in creation order; a dropped connection mid-sync stops
 * the run so the remaining actions are retried later. Server rejections (4xx)
 * are recorded on the action and retried on the next sync.
 */
@Singleton
class OfflineSyncManager @Inject constructor(
    private val retrofitClient: RetrofitClient,
    private val offlineStore: OfflineStore,
    private val connectivity: ConnectivityObserver,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private var started = false

    /** Starts observing connectivity (idempotent). */
    fun start() {
        if (started) return
        started = true
        scope.launch {
            connectivity.isOnline.collect { online ->
                if (online) {
                    refreshPatientCache()
                    syncNow()
                }
            }
        }
    }

    suspend fun syncNow(): SyncResult {
        if (!connectivity.online) return SyncResult(offline = true)
        _isSyncing.value = true
        var synced = 0
        var failed = 0
        // Local placeholder ids → server ids, so dependent writes resolve.
        val patientIdMap = mutableMapOf<String, String>()
        try {
            for (action in offlineStore.pendingActions()) {
                if (!connectivity.online) return SyncResult(synced, failed, offline = true)
                try {
                    replay(action, patientIdMap)
                    offlineStore.completeAction(action.id)
                    synced++
                } catch (e: IOException) {
                    // Connection dropped mid-flight — keep the queue intact.
                    offlineStore.failAction(action.id, e.message)
                    return SyncResult(synced, failed, offline = true)
                } catch (e: Exception) {
                    offlineStore.failAction(action.id, e.message ?: "Sync failed")
                    failed++
                }
            }
        } finally {
            _isSyncing.value = false
        }
        return SyncResult(synced = synced, failed = failed)
    }

    private suspend fun replay(
        action: PendingActionEntity,
        patientIdMap: MutableMap<String, String>,
    ) {
        val apis = retrofitClient.apis.value
        when (action.type) {
            PendingActionType.PATIENT -> {
                val dto = json.decodeFromString<CreatePatientDto>(action.payload)
                val response = apis.patientApi.create(dto)
                if (!response.isSuccessful) {
                    throw IllegalStateException("Patient sync failed (${response.code()})")
                }
                val created = response.body()
                if (created != null) {
                    offlineStore.cachePatient(created)
                    // Map the local placeholder (id + synthetic MRN) to the server
                    // values so queued appointments/documents resolve correctly.
                    val local = action.localRef?.let { offlineStore.patient(it) }
                    if (local != null) {
                        patientIdMap[local.id] = created.id.ifBlank { created.patientId }
                        if (local.patientId.isNotBlank()) {
                            patientIdMap[local.patientId] = created.patientId.ifBlank { created.id }
                        }
                    }
                    action.localRef?.let { offlineStore.deleteLocalPatient(it) }
                }
            }

            PendingActionType.APPOINTMENT -> {
                val dto = json.decodeFromString<CreateAppointmentDto>(action.payload)
                val resolvedPatientId = patientIdMap[dto.patientId] ?: dto.patientId
                val response = apis.appointmentApi.create(dto.copy(patientId = resolvedPatientId))
                if (!response.isSuccessful) {
                    throw IllegalStateException("Appointment sync failed (${response.code()})")
                }
            }

            PendingActionType.DOCUMENT -> {
                val dto = json.decodeFromString<CreateFormSubmissionDto>(action.payload)
                val resolvedPatientId = patientIdMap[dto.patientId] ?: dto.patientId
                val response = apis.formApi.createSubmission(dto.copy(patientId = resolvedPatientId))
                if (!response.isSuccessful) {
                    throw IllegalStateException("Document sync failed (${response.code()})")
                }
            }

            else -> throw IllegalStateException("Unknown offline action: ${action.type}")
        }
    }

    /**
     * Pulls the patient directory into the device cache so searches work
     * offline. Pages until exhausted (capped to protect large orgs).
     */
    suspend fun refreshPatientCache() {
        if (!connectivity.online) return
        var page = 1
        var cached = 0
        while (page <= MAX_PAGES && cached < MAX_PATIENTS) {
            val response = runCatching {
                retrofitClient.apis.value.patientApi.list(
                    page = page,
                    limit = PAGE_SIZE,
                    sortBy = "createdAt",
                    sortOrder = "desc",
                )
            }.getOrNull() ?: return
            if (!response.isSuccessful) return
            val rows = response.body()?.data.orEmpty()
            if (rows.isEmpty()) return
            offlineStore.cachePatients(rows)
            cached += rows.size
            if (rows.size < PAGE_SIZE) return
            page++
        }
    }

    private companion object {
        const val PAGE_SIZE = 100
        const val MAX_PAGES = 50
        const val MAX_PATIENTS = 5_000
    }
}
