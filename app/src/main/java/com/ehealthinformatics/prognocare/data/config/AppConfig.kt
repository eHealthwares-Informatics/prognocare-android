package com.ehealthinformatics.prognocare.data.config

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Serializable
enum class QueryRangePeriod(val id: String, val label: String) {
    /** Calendar today (default when no range is configured). */
    TODAY("today", "Today"),
    /** Last 1 day ending today. */
    LAST_DAY("last_day", "Last day"),
    /** Last 7 days ending today. */
    LAST_WEEK("last_week", "Last week"),
    /** Last 30 days ending today. */
    LAST_MONTH("last_month", "Last month"),
    ;

    companion object {
        fun fromId(id: String?): QueryRangePeriod? =
            entries.firstOrNull { it.id == id }
    }
}

@Serializable
data class AppConfig(
    val emrBaseUrl: String = AppConfigStore.DEFAULT_EMR_URL,
    val conversationBaseUrl: String = AppConfigStore.DEFAULT_CONVERSATION_URL,
    /** Stable conversation-engine channel code (NOT a DB id). */
    val webChannelCode: String = AppConfigStore.DEFAULT_WEB_CHANNEL_CODE,
    /**
     * Relative query window preset. Dates are resolved dynamically at query
     * time (not stored as raw start/end). Null = default (today).
     */
    val queryRange: QueryRangePeriod? = null,
    /** Server environment preset used to fill base URLs. */
    val serverEnvironment: ServerEnvironment = ServerEnvironment.DEVELOPMENT,
)

@Serializable
enum class ServerEnvironment(val id: String, val label: String) {
    PRODUCTION("production", "Production"),
    DEVELOPMENT("development", "Development"),
    ;

    /**
     * Production gateway (api.ehealthwares.com) strips the service prefix and
     * Nest adds `/api` internally — external paths are `/emr/...` and
     * `/conversation/...` (no extra `api/` segment). Health:
     * - EMR: `https://api.ehealthwares.com/emr/health`
     * - Conversation: `https://api.ehealthwares.com/conversation/health`
     * Trailing slash required by Retrofit.
     */
    val emrBaseUrl: String
        get() = when (this) {
            PRODUCTION -> "https://api.ehealthwares.com/emr/"
            DEVELOPMENT -> AppConfigStore.DEFAULT_EMR_URL
        }

    val conversationBaseUrl: String
        get() = when (this) {
            // conversation.ehealthwares.com serves the web SPA, not the API.
            PRODUCTION -> "https://api.ehealthwares.com/conversation/"
            DEVELOPMENT -> AppConfigStore.DEFAULT_CONVERSATION_URL
        }

    companion object {
        fun fromId(id: String?): ServerEnvironment =
            entries.firstOrNull { it.id == id } ?: DEVELOPMENT
    }
}

val AppConfig.conversationSocketUrl: String
    get() = conversationBaseUrl
        .trimEnd('/')
        .replace(Regex("/api/?$"), "")

/**
 * Resolves the configured period to concrete `yyyy-MM-dd` bounds **now**.
 * Returns null when no period is set (callers fall back to today).
 */
fun AppConfig.resolveQueryDateRange(today: LocalDate = LocalDate.now()): QueryDateRange? {
    val period = queryRange ?: return null
    return period.resolve(today)
}

fun QueryRangePeriod.resolve(today: LocalDate = LocalDate.now()): QueryDateRange =
    when (this) {
        QueryRangePeriod.TODAY ->
            QueryDateRange(start = today.toString(), end = today.toString())
        QueryRangePeriod.LAST_DAY ->
            QueryDateRange(start = today.toString(), end = today.toString())
        QueryRangePeriod.LAST_WEEK ->
            QueryDateRange(
                start = today.minus(6, ChronoUnit.DAYS).toString(),
                end = today.toString(),
            )
        QueryRangePeriod.LAST_MONTH ->
            QueryDateRange(
                start = today.minus(29, ChronoUnit.DAYS).toString(),
                end = today.toString(),
            )
    }

/**
 * Concrete query window (dynamic). Maps to EMR list-filter DSL.
 */
data class QueryDateRange(
    val start: String?,
    val end: String?,
) {
    val isSet: Boolean get() = start != null || end != null

    fun toAppointmentDateParam(): String? = when {
        start != null && end != null && start == end -> start
        start != null && end != null -> "BETWEEN|$start|$end"
        start != null -> "GREATER_THAN_OR_EQUAL|$start"
        end != null -> "LESS_THAN_OR_EQUAL|$end"
        else -> null
    }

    fun displayLabel(): String = when {
        start != null && end != null && start == end -> start!!
        start != null && end != null -> "$start → $end"
        start != null -> "From $start"
        end != null -> "Until $end"
        else -> "Not set"
    }
}

fun AppConfig.withEmrBaseUrl(raw: String): AppConfig {
    val normalized = raw.trim().trimEnd('/').ifEmpty { emrBaseUrl.trimEnd('/') }
    val withSlash = if (normalized.endsWith("/")) normalized else "$normalized/"
    return copy(emrBaseUrl = withSlash)
}

fun AppConfig.withConversationBaseUrl(raw: String): AppConfig {
    val normalized = raw.trim().trimEnd('/').ifEmpty { conversationBaseUrl.trimEnd('/') }
    val withSlash = if (normalized.endsWith("/")) normalized else "$normalized/"
    return copy(conversationBaseUrl = withSlash)
}

fun AppConfig.withWebChannelCode(raw: String): AppConfig {
    val normalized = raw.trim().ifEmpty { webChannelCode }
    return copy(webChannelCode = normalized)
}

/** Sets the relative query period (null clears → default today). */
fun AppConfig.withQueryRange(period: QueryRangePeriod?): AppConfig =
    copy(queryRange = period)

/** Applies the environment preset URLs (keeps webChannelCode). */
fun AppConfig.withServerEnvironment(env: ServerEnvironment): AppConfig =
    copy(
        serverEnvironment = env,
        emrBaseUrl = env.emrBaseUrl,
        conversationBaseUrl = env.conversationBaseUrl,
    )
