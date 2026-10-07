package com.ehealthinformatics.prognocare

import com.ehealthinformatics.prognocare.data.config.AppConfig
import com.ehealthinformatics.prognocare.data.config.QueryRangePeriod
import com.ehealthinformatics.prognocare.data.config.ServerEnvironment
import com.ehealthinformatics.prognocare.data.config.conversationSocketUrl
import com.ehealthinformatics.prognocare.data.config.resolve
import com.ehealthinformatics.prognocare.data.config.resolveQueryDateRange
import com.ehealthinformatics.prognocare.data.config.withConversationBaseUrl
import com.ehealthinformatics.prognocare.data.config.withEmrBaseUrl
import com.ehealthinformatics.prognocare.data.config.withQueryRange
import com.ehealthinformatics.prognocare.data.config.withServerEnvironment
import com.ehealthinformatics.prognocare.data.config.withWebChannelCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class AppConfigTest {

    private val config = AppConfig(
        emrBaseUrl = "http://10.0.2.2:8093/",
        conversationBaseUrl = "http://10.0.2.2:8090/api",
        webChannelCode = "PROGNOCARE_MESSAGING",
    )

    @Test
    fun `conversation socket url strips api prefix`() {
        assertEquals("http://10.0.2.2:8090", config.conversationSocketUrl)
    }

    @Test
    fun `emr url normalization adds trailing slash`() {
        assertEquals(
            "http://192.168.1.10:8093/",
            config.withEmrBaseUrl("http://192.168.1.10:8093").emrBaseUrl,
        )
    }

    @Test
    fun `empty emr url keeps previous value`() {
        assertEquals(config.emrBaseUrl, config.withEmrBaseUrl("   ").emrBaseUrl)
    }

    @Test
    fun `production environment fills ehealthwares gateway urls`() {
        val prod = config.withServerEnvironment(ServerEnvironment.PRODUCTION)
        // Gateway strips /emr and /conversation; Nest serves /api internally.
        assertEquals("https://api.ehealthwares.com/emr", prod.emrBaseUrl)
        assertEquals("https://api.ehealthwares.com/conversation", prod.conversationBaseUrl)
        assertEquals(ServerEnvironment.PRODUCTION, prod.serverEnvironment)
    }

    @Test
    fun `development environment restores lan defaults`() {
        val dev = config
            .withServerEnvironment(ServerEnvironment.PRODUCTION)
            .withServerEnvironment(ServerEnvironment.DEVELOPMENT)
        assertEquals(ServerEnvironment.DEVELOPMENT, dev.serverEnvironment)
        assertEquals(AppConfigStoreDefaults.emr, dev.emrBaseUrl)
    }

    @Test
    fun `query range null resolves to null (default today)`() {
        assertNull(config.resolveQueryDateRange(LocalDate.of(2026, 10, 7)))
    }

    @Test
    fun `last week resolves to 7 days ending today`() {
        val today = LocalDate.of(2026, 10, 7)
        val range = QueryRangePeriod.LAST_WEEK.resolve(today)
        assertEquals("2026-10-01", range.start)
        assertEquals("2026-10-07", range.end)
        assertEquals("BETWEEN|2026-10-01|2026-10-07", range.toAppointmentDateParam())
    }

    @Test
    fun `last month resolves to 30 days ending today`() {
        val today = LocalDate.of(2026, 10, 7)
        val range = QueryRangePeriod.LAST_MONTH.resolve(today)
        assertEquals("2026-09-08", range.start)
        assertEquals("2026-10-07", range.end)
    }

    @Test
    fun `today range is a single day param`() {
        val today = LocalDate.of(2026, 10, 7)
        val range = QueryRangePeriod.TODAY.resolve(today)
        assertEquals("2026-10-07", range.toAppointmentDateParam())
    }

    @Test
    fun `withQueryRange stores period id`() {
        val updated = config.withQueryRange(QueryRangePeriod.LAST_WEEK)
        assertEquals(QueryRangePeriod.LAST_WEEK, updated.queryRange)
        assertEquals(
            QueryRangePeriod.LAST_WEEK,
            updated.withQueryRange(null).queryRange,
        )
    }

    @Test
    fun `conversation url normalization keeps path`() {
        assertEquals(
            "http://192.168.1.20:8090/api",
            config.withConversationBaseUrl("http://192.168.1.20:8090/api/").conversationBaseUrl,
        )
    }

    @Test
    fun `empty web channel code keeps previous value`() {
        assertEquals(config.webChannelCode, config.withWebChannelCode("  ").webChannelCode)
    }
}

/** Mirrors AppConfigStore defaults without depending on BuildConfig in unit tests. */
private object AppConfigStoreDefaults {
    val emr: String
        get() = com.ehealthinformatics.prognocare.data.config.AppConfigStore.DEFAULT_EMR_URL
}
