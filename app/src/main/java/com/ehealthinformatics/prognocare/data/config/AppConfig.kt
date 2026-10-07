package com.ehealthinformatics.prognocare.data.config

import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data class AppConfig(
    val emrBaseUrl: String = AppConfigStore.DEFAULT_EMR_URL,
    val conversationBaseUrl: String = AppConfigStore.DEFAULT_CONVERSATION_URL,
    /** Stable conversation-engine channel code (NOT a DB id). */
    val webChannelCode: String = AppConfigStore.DEFAULT_WEB_CHANNEL_CODE,
    /**
     * Global query date-range start (`yyyy-MM-dd`). Null = not set.
     * When set (with or without end), appointment queries use this range
     * instead of a hardcoded "today".
     */
    val queryDateStart: String? = null,
    /** Global query date-range end (`yyyy-MM-dd`). Null = open-ended. */
    val queryDateEnd: String? = null,
)

val AppConfig.conversationSocketUrl: String
    get() = conversationBaseUrl
        .trimEnd('/')
        .replace(Regex("/api/?$"), "")

/** Configured query date range, or null when neither bound is set. */
fun AppConfig.queryDateRange(): QueryDateRange? {
    val start = queryDateStart?.takeIf { it.isNotBlank() }
    val end = queryDateEnd?.takeIf { it.isNotBlank() }
    if (start == null && end == null) return null
    return QueryDateRange(start = start, end = end)
}

/**
 * User-configured query window from Settings.
 * Maps to the EMR list-filter DSL (`BETWEEN|from|to`, etc.).
 */
data class QueryDateRange(
    val start: String?,
    val end: String?,
) {
    val isSet: Boolean get() = start != null || end != null

    /**
     * EMR appointment `date` query parameter.
     * Both bounds → `BETWEEN|start|end`; one bound → gte/lte; none → null.
     */
    fun toAppointmentDateParam(): String? = when {
        start != null && end != null -> "BETWEEN|$start|$end"
        start != null -> "GREATER_THAN_OR_EQUAL|$start"
        end != null -> "LESS_THAN_OR_EQUAL|$end"
        else -> null
    }

    fun displayLabel(): String = when {
        start != null && end != null -> "$start → $end"
        start != null -> "From $start"
        end != null -> "Until $end"
        else -> "Not set"
    }
}

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

fun AppConfig.withQueryDateRange(start: String?, end: String?): AppConfig {
    val normalizedStart = start?.trim()?.takeIf { it.isNotBlank() }
    val normalizedEnd = end?.trim()?.takeIf { it.isNotBlank() }
    return copy(queryDateStart = normalizedStart, queryDateEnd = normalizedEnd)
}

/** Validates `yyyy-MM-dd` (strict ISO local date). */
fun isValidIsoDate(value: String?): Boolean {
    if (value.isNullOrBlank()) return false
    return try {
        LocalDate.parse(value)
        true
    } catch (_: Exception) {
        false
    }
}
