package com.ehealthinformatics.prognocare.data.location

import android.content.Context
import com.ehealthinformatics.prognocare.data.auth.SessionStore
import com.ehealthinformatics.prognocare.data.remote.RetrofitClient
import com.ehealthinformatics.prognocare.data.remote.models.Location
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/**
 * The location a user's list queries are scoped to ("location based query").
 *
 * Resolution order:
 *  1. the location explicitly picked in the UI ([SessionStore.getActiveLocationId]),
 *  2. the location of the signed-in user's staff record, saved at login,
 *  3. null — meaning "All locations" (queries are not filtered).
 *
 * The scope id is stored in the `prognocare_auth` prefs so signing out clears it.
 */
@Singleton
class LocationScope @Inject constructor(
    @ApplicationContext private val context: Context,
    private val retrofitClient: RetrofitClient,
) {
    data class Scope(
        val id: String?,
        val name: String?,
    ) {
        val isAll: Boolean get() = id == null
    }

    /** Currently active scope, defaulting to the staff record's location. */
    fun current(): Scope {
        val id = SessionStore.getActiveLocationId(context)
            ?: SessionStore.getStaffLocationId(context)
        return Scope(id, SessionStore.getActiveLocationName(context))
    }

    fun setActive(id: String?, name: String?) {
        SessionStore.saveActiveLocation(context, id, name)
    }

    fun clearActive() {
        SessionStore.saveActiveLocation(context, null, null)
    }

    /** Fetch the location record for an id (null when unreachable). */
    suspend fun location(id: String): Location? {
        if (id.isBlank()) return null
        return runCatching {
            retrofitClient.apis.first().locationApi.getById(id).body()
        }.getOrNull()
    }

    /** All active locations, used by the location picker. */
    suspend fun locations(search: String? = null, limit: Int = 100): List<Location> {
        return runCatching {
            retrofitClient.apis.first().locationApi
                .list(search = search, limit = limit)
                .body()
                ?.data.orEmpty()
                .filter { it.isActive }
        }.getOrDefault(emptyList())
    }

    companion object {
        /**
         * The one location-match rule used by every scoped list: a null scope
         * matches everything, otherwise the item's location (or its fallback
         * display value) must equal the scope id.
         */
        fun matches(scopeId: String?, itemLocationId: String?, fallback: String? = null): Boolean =
            scopeId == null || (itemLocationId ?: fallback) == scopeId
    }
}
