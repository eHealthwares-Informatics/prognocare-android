package com.ehealthinformatics.prognocare.data.config

import com.ehealthinformatics.prognocare.data.remote.api.HealthApi
import com.ehealthinformatics.prognocare.data.remote.models.HealthStatus
import com.ehealthinformatics.prognocare.data.remote.api.ConversationHealthApi
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import javax.inject.Inject
import javax.inject.Singleton

sealed class ConnectionCheck {
    abstract val label: String

    data class Success(override val label: String) : ConnectionCheck()
    data class Failure(override val label: String, val detail: String) : ConnectionCheck()
}

/**
 * Validates a candidate config against live endpoints. Used by the settings
 * flow to confirm EMR + Conversation Engine URLs before persisting them.
 *
 * Health paths (after stripping a trailing slash from the base URL):
 * - LAN/dev: `{base}/api/health` (Nest global prefix)
 * - Production gateway: `{base}/health` where base is
 *   `https://api.ehealthwares.com/emr` or `.../conversation`
 *   (gateway strips the service prefix; Nest still serves `/api/health`
 *   internally, exposed externally as `/health`).
 */
@Singleton
class ServerConfigVerifier @Inject constructor() {

    private val json = Json { ignoreUnknownKeys = true }

    private fun retrofit(baseUrl: String): Retrofit {
        val client = OkHttpClient.Builder()
            .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
            .build()
        return Retrofit.Builder()
            .baseUrl(baseUrl.trimEnd('/') + "/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    suspend fun verify(config: AppConfig): List<ConnectionCheck> {
        return listOf(checkEmr(config.emrBaseUrl), checkConversation(config.conversationBaseUrl))
    }

    private suspend fun checkEmr(baseUrl: String): ConnectionCheck {
        return try {
            val health = retrofit(baseUrl).create(HealthApi::class.java).health()
            if (isHealthy(health)) {
                ConnectionCheck.Success("EMR")
            } else {
                ConnectionCheck.Failure(
                    "EMR",
                    "unexpected response (${health.code()})",
                )
            }
        } catch (e: Exception) {
            ConnectionCheck.Failure("EMR", e.message ?: "unreachable")
        }
    }

    private suspend fun checkConversation(baseUrl: String): ConnectionCheck {
        return try {
            val health = retrofit(baseUrl).create(ConversationHealthApi::class.java).health()
            if (isHealthy(health)) {
                ConnectionCheck.Success("Conversation engine")
            } else {
                ConnectionCheck.Failure(
                    "Conversation engine",
                    "unexpected response (${health.code()})",
                )
            }
        } catch (e: Exception) {
            ConnectionCheck.Failure("Conversation engine", e.message ?: "unreachable")
        }
    }

    /**
     * Accept any 2xx JSON body that carries a `status` field — production
     * gateways may return `{status:"ok", service:"..."}` while LAN Nest
     * returns the same shape; HTML SPA fallbacks fail parsing.
     */
    private fun isHealthy(response: retrofit2.Response<HealthStatus>): Boolean {
        if (!response.isSuccessful) return false
        val body = response.body() ?: return false
        return body.status.equals("ok", ignoreCase = true) ||
            body.status.equals("healthy", ignoreCase = true)
    }
}