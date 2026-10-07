package com.ehealthinformatics.prognocare.feature.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ehealthinformatics.prognocare.data.remote.models.NotificationItem
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class NotificationsUiState(
    val unreadCount: Int = 0,
    val items: List<NotificationItem> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isBusy: Boolean = false,
    val error: String? = null,
    /** Latest `createdAt` seen — poll cursor for the next delta fetch. */
    val sinceCursor: String? = null,
)

/**
 * Feed + badge state for EMR in-app notifications.
 *
 * - Dashboard open: best-effort subscription + unread refresh (never throws).
 * - Feed visible: initial full load, then 30s poll with `since` cursor,
 *   merging items by id (newer read state wins).
 */
@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val repository: NotificationsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(NotificationsUiState())
    val state: StateFlow<NotificationsUiState> = _state.asStateFlow()

    private var pollJob: Job? = null
    private var subscribed = false

    init {
        // Best-effort on first use (e.g. shared VM created by a dashboard).
        registerSubscription()
        refreshUnread()
    }

    /**
     * Called when a role dashboard becomes visible: heartbeats the
     * subscription (404/5xx swallowed) and refreshes the unread badge.
     */
    fun onDashboardOpen() {
        registerSubscription()
        refreshUnread()
    }

    /** Called when the notifications feed becomes visible. */
    fun onFeedOpened() {
        loadInitial()
        startPolling()
    }

    /** Called when the feed is no longer visible. */
    fun onFeedClosed() {
        stopPolling()
    }

    fun refresh() {
        when {
            _state.value.items.isEmpty() -> loadInitial()
            else -> pollOnce(isUserRefresh = true)
        }
    }

    fun retry() {
        _state.value = _state.value.copy(error = null)
        refresh()
    }

    fun markRead(id: String) {
        if (_state.value.isBusy) return
        viewModelScope.launch {
            _state.value = _state.value.copy(isBusy = true, error = null)
            try {
                val result = repository.markRead(id)
                val updatedItems = _state.value.items.map {
                    if (it.id == id) it.copy(read = result.read, readAt = result.readAt) else it
                }
                _state.value = _state.value.copy(
                    items = updatedItems,
                    unreadCount = updatedItems.count { !it.read },
                )
                refreshUnread()
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message ?: "Could not mark read")
            } finally {
                _state.value = _state.value.copy(isBusy = false)
            }
        }
    }

    fun markAllRead() {
        if (_state.value.isBusy) return
        viewModelScope.launch {
            _state.value = _state.value.copy(isBusy = true, error = null)
            try {
                repository.markAllRead()
                _state.value = _state.value.copy(
                    items = _state.value.items.map { it.copy(read = true) },
                    unreadCount = 0,
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message ?: "Could not mark all read")
            } finally {
                _state.value = _state.value.copy(isBusy = false)
            }
        }
    }

    private fun registerSubscription() {
        if (subscribed) return
        viewModelScope.launch {
            try {
                repository.subscribe()
                subscribed = true
            } catch (_: Exception) {
                // Best-effort: missing endpoint / offline must not block the UI.
            }
        }
    }

    private fun refreshUnread() {
        viewModelScope.launch {
            try {
                val count = repository.unreadCount()
                _state.value = _state.value.copy(unreadCount = count, error = null)
            } catch (_: Exception) {
                // Badge stays at the last known value on failure.
            }
        }
    }

    private fun loadInitial() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, isRefreshing = true, error = null)
            try {
                val items = repository.list(since = null)
                _state.value = _state.value.copy(
                    items = mergeById(existing = emptyList(), incoming = items),
                    sinceCursor = items.maxByOrNull { it.createdAt ?: "" }?.createdAt
                        ?: _state.value.sinceCursor,
                    unreadCount = items.count { !it.read },
                    isLoading = false,
                    isRefreshing = false,
                )
                refreshUnread()
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    error = e.message ?: "Failed to load notifications",
                )
            }
        }
    }

    private fun startPolling() {
        stopPolling()
        pollJob = viewModelScope.launch {
            while (isActive) {
                delay(POLL_INTERVAL_MS)
                pollOnce(isUserRefresh = false)
            }
        }
    }

    private fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
    }

    private fun pollOnce(isUserRefresh: Boolean) {
        viewModelScope.launch {
            if (isUserRefresh) {
                _state.value = _state.value.copy(isRefreshing = true)
            }
            try {
                val since = if (isUserRefresh) null else _state.value.sinceCursor
                val incoming = repository.list(since = since)
                val merged = mergeById(_state.value.items, incoming)
                _state.value = _state.value.copy(
                    items = merged,
                    sinceCursor = (incoming.mapNotNull { it.createdAt } +
                        _state.value.items.mapNotNull { it.createdAt })
                        .maxOrNull() ?: _state.value.sinceCursor,
                    isRefreshing = false,
                    error = null,
                )
                refreshUnread()
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isRefreshing = false,
                    error = if (isUserRefresh) e.message ?: "Failed to refresh" else null,
                )
            }
        }
    }

    /**
     * Merge by id. Incoming rows win (they carry the latest read state from
     * the server); order stays newest-first by createdAt.
     */
    private fun mergeById(
        existing: List<NotificationItem>,
        incoming: List<NotificationItem>,
    ): List<NotificationItem> {
        val byId = LinkedHashMap<String, NotificationItem>()
        existing.forEach { byId[it.id] = it }
        incoming.forEach { byId[it.id] = it }
        return byId.values
            .sortedByDescending { it.createdAt ?: "" }
            .take(FEED_LIMIT)
    }

    companion object {
        const val POLL_INTERVAL_MS = 30_000L
        const val FEED_LIMIT = 50
    }
}
