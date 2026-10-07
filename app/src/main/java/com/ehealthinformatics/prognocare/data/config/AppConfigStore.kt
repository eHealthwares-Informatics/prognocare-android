package com.ehealthinformatics.prognocare.data.config

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ehealthinformatics.prognocare.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

val Context.appConfigDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_config")

/**
 * Runtime config store for server URLs + query date range. Backed by
 * DataStore for persistence but kept as an in-memory StateFlow so clients
 * can rebuild on change.
 */
class AppConfigStore(private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val config: StateFlow<AppConfig> = context.appConfigDataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            with(default()) {
                AppConfig(
                    emrBaseUrl = prefs[Keys.EMR_BASE_URL] ?: emrBaseUrl,
                    conversationBaseUrl = prefs[Keys.CONVERSATION_BASE_URL] ?: conversationBaseUrl,
                    webChannelCode = prefs[Keys.WEB_CHANNEL_CODE] ?: webChannelCode,
                    queryDateStart = prefs[Keys.QUERY_DATE_START]
                        ?.takeIf { it.isNotBlank() },
                    queryDateEnd = prefs[Keys.QUERY_DATE_END]
                        ?.takeIf { it.isNotBlank() },
                )
            }
        }
        .stateIn(scope, SharingStarted.Eagerly, default())

    suspend fun updateConfig(config: AppConfig) {
        context.appConfigDataStore.edit { prefs ->
            prefs[Keys.EMR_BASE_URL] = config.emrBaseUrl
            prefs[Keys.CONVERSATION_BASE_URL] = config.conversationBaseUrl
            prefs[Keys.WEB_CHANNEL_CODE] = config.webChannelCode
            // Empty string = cleared (DataStore has no null for string keys).
            prefs[Keys.QUERY_DATE_START] = config.queryDateStart.orEmpty()
            prefs[Keys.QUERY_DATE_END] = config.queryDateEnd.orEmpty()
        }
    }

    suspend fun resetToDefaults() {
        updateConfig(default())
    }

    private fun default(): AppConfig = AppConfig()

    private object Keys {
        val EMR_BASE_URL = stringPreferencesKey("emr_base_url")
        val CONVERSATION_BASE_URL = stringPreferencesKey("conversation_base_url")
        val WEB_CHANNEL_CODE = stringPreferencesKey("web_channel_code")
        val QUERY_DATE_START = stringPreferencesKey("query_date_start")
        val QUERY_DATE_END = stringPreferencesKey("query_date_end")
    }

    companion object {
        var DEFAULT_EMR_URL = BuildConfig.DEFAULT_EMR_URL
        var DEFAULT_CONVERSATION_URL = BuildConfig.DEFAULT_CONVERSATION_URL
        var DEFAULT_WEB_CHANNEL_CODE = BuildConfig.DEFAULT_WEB_CHANNEL_CODE
    }
}
