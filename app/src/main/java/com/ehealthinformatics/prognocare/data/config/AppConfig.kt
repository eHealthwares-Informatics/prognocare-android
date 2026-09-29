package com.ehealthinformatics.prognocare.data.config

import kotlinx.serialization.Serializable

@Serializable
data class AppConfig(
    val emrBaseUrl: String = AppConfigStore.DEFAULT_EMR_URL,
    val conversationBaseUrl: String = AppConfigStore.DEFAULT_CONVERSATION_URL,
    /** Stable conversation-engine channel code (NOT a DB id). */
    val webChannelCode: String = AppConfigStore.DEFAULT_WEB_CHANNEL_CODE,
)

val AppConfig.conversationSocketUrl: String
    get() = conversationBaseUrl
        .trimEnd('/')
        .replace(Regex("/api/?$"), "")

fun AppConfig.withEmrBaseUrl(raw: String): AppConfig {
    val normalized = raw.trim().trimEnd('/').ifEmpty { emrBaseUrl }
    val withSlash = if (normalized.endsWith("/")) normalized else "$normalized/"
    return copy(emrBaseUrl = withSlash)
}

fun AppConfig.withConversationBaseUrl(raw: String): AppConfig {
    val normalized = raw.trim().trimEnd('/').ifEmpty { conversationBaseUrl }
    return copy(conversationBaseUrl = normalized)
}

fun AppConfig.withWebChannelCode(raw: String): AppConfig {
    val normalized = raw.trim().ifEmpty { webChannelCode }
    return copy(webChannelCode = normalized)
}
