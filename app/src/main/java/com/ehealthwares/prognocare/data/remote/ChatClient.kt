package com.ehealthwares.prognocare.data.remote

import com.ehealthwares.prognocare.data.config.AppConfigStore
import com.ehealthwares.prognocare.data.remote.api.ChatApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Retrofit service for the Conversation Engine (chat). Rebuilt whenever the
 * conversation base URL config changes.
 */
@Singleton
class ChatClient @Inject constructor(
    private val authInterceptor: AuthInterceptor,
    private val configStore: AppConfigStore,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

    private fun buildHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
        )
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private fun buildChatApi(baseUrl: String): ChatApi {
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl + "/")
            .client(buildHttpClient())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        return retrofit.create(ChatApi::class.java)
    }

    /** Current chat API, rebuilt when the conversation base URL changes. */
    val chatApi: StateFlow<ChatApi> = configStore.config
        .map { config -> buildChatApi(config.conversationBaseUrl) }
        .stateIn(scope, SharingStarted.Eagerly, buildChatApi(AppConfigStore.DEFAULT_CONVERSATION_URL))
}