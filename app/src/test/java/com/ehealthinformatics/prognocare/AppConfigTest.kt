package com.ehealthinformatics.prognocare

import com.ehealthinformatics.prognocare.data.config.AppConfig
import com.ehealthinformatics.prognocare.data.config.conversationSocketUrl
import com.ehealthinformatics.prognocare.data.config.isValidIsoDate
import com.ehealthinformatics.prognocare.data.config.queryDateRange
import com.ehealthinformatics.prognocare.data.config.withConversationBaseUrl
import com.ehealthinformatics.prognocare.data.config.withEmrBaseUrl
import com.ehealthinformatics.prognocare.data.config.withQueryDateRange
import com.ehealthinformatics.prognocare.data.config.withWebChannelCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

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
    fun `conversation socket url handles trailing slash`() {
        val withSlash = config.copy(conversationBaseUrl = "http://10.0.2.2:8090/api/")
        assertEquals("http://10.0.2.2:8090", withSlash.conversationSocketUrl)
    }

    @Test
    fun `emr url normalization adds trailing slash`() {
        val updated = config.withEmrBaseUrl("http://192.168.1.10:8093")
        assertEquals("http://192.168.1.10:8093/", updated.emrBaseUrl)
    }

    @Test
    fun `emr url normalization keeps trailing slash`() {
        val updated = config.withEmrBaseUrl("http://192.168.1.10:8093/")
        assertEquals("http://192.168.1.10:8093/", updated.emrBaseUrl)
    }

    @Test
    fun `empty emr url keeps previous value`() {
        val updated = config.withEmrBaseUrl("   ")
        assertEquals(config.emrBaseUrl, updated.emrBaseUrl)
    }

    @Test
    fun `conversation url normalization trims trailing slash`() {
        val updated = config.withConversationBaseUrl("http://192.168.1.20:8090/api/")
        assertEquals("http://192.168.1.20:8090/api", updated.conversationBaseUrl)
    }

    @Test
    fun `empty conversation url keeps previous value`() {
        val updated = config.withConversationBaseUrl("  ")
        assertEquals(config.conversationBaseUrl, updated.conversationBaseUrl)
    }

    @Test
    fun `empty web channel code keeps previous value`() {
        val updated = config.withWebChannelCode("  ")
        assertEquals(config.webChannelCode, updated.webChannelCode)
    }

    @Test
    fun `query date range is null when unset`() {
        assertNull(config.queryDateRange())
    }

    @Test
    fun `query date range maps both bounds to BETWEEN DSL`() {
        val updated = config.withQueryDateRange("2026-01-01", "2026-01-31")
        val range = updated.queryDateRange()
        assertEquals("BETWEEN|2026-01-01|2026-01-31", range?.toAppointmentDateParam())
    }

    @Test
    fun `query date range maps start-only to gte DSL`() {
        val updated = config.withQueryDateRange("2026-02-01", null)
        assertEquals(
            "GREATER_THAN_OR_EQUAL|2026-02-01",
            updated.queryDateRange()?.toAppointmentDateParam(),
        )
    }

    @Test
    fun `query date range clear resets both bounds`() {
        val updated = config
            .withQueryDateRange("2026-01-01", "2026-01-31")
            .withQueryDateRange(null, null)
        assertNull(updated.queryDateStart)
        assertNull(updated.queryDateEnd)
        assertNull(updated.queryDateRange())
    }

    @Test
    fun `iso date validation`() {
        assertTrue(isValidIsoDate("2026-10-07"))
        assertFalse(isValidIsoDate("07/10/2026"))
        assertFalse(isValidIsoDate(null))
        assertFalse(isValidIsoDate("not-a-date"))
    }
}