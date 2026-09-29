package com.ehealthinformatics.prognocare

import com.ehealthinformatics.prognocare.data.remote.models.FormSubmission
import com.ehealthinformatics.prognocare.feature.forms.VitalsReadings
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Tests extraction of vitals values from VITALS form submissions. */
class VitalsReadingsTest {

    private fun submission(
        id: String,
        formName: String,
        data: kotlinx.serialization.json.JsonObject,
        status: String = "SUBMITTED",
        submittedAt: String? = "2026-09-24T09:00:00Z",
    ) = FormSubmission(
        id = id,
        formName = formName,
        dataJson = data,
        status = status,
        submittedAt = submittedAt,
        submittedByName = "Nurse Amara",
    )

    private val seedSchemaData = buildJsonObject {
        put("temperature", 37.5)
        put("heartRate", 72)
        put("respiratoryRate", 16)
        put("bloodPressureSystolic", 120)
        put("bloodPressureDiastolic", 80)
        put("oxygenSaturation", 98)
        put("weight", 70.5)
        put("height", 175)
        put("notes", "Patient stable")
    }

    @Test
    fun `extracts readings with units from the seeded camelCase schema`() {
        val vitals = VitalsReadings.from(submission("s1", "Vitals", seedSchemaData))!!

        assertEquals("37.5 °C", vitals.temperature)
        assertEquals("120/80 mmHg", vitals.bloodPressure)
        assertEquals("72 bpm", vitals.heartRate)
        assertEquals("16 /min", vitals.respiratoryRate)
        assertEquals("98 %", vitals.oxygenSaturation)
        assertEquals("70.5 kg", vitals.weight)
        assertEquals("175 cm", vitals.height)
        assertEquals("Patient stable", vitals.notes)
        assertEquals("Nurse Amara", vitals.recordedBy)
    }

    @Test
    fun `works with snake_case payloads`() {
        val data = buildJsonObject {
            put("temperature", 36.8)
            put("heart_rate", 88)
            put("bp_systolic", 130)
            put("bp_diastolic", 85)
            put("oxygen_saturation", 95)
        }
        val vitals = VitalsReadings.from(submission("s2", "VITALS", data))!!

        assertEquals("36.8 °C", vitals.temperature)
        assertEquals("88 bpm", vitals.heartRate)
        assertEquals("130/85 mmHg", vitals.bloodPressure)
        assertEquals("95 %", vitals.oxygenSaturation)
    }

    @Test
    fun `bp falls back to a one-sided reading and numbers drop trailing zeros`() {
        val data = buildJsonObject { put("bloodPressureSystolic", 140) }
        val vitals = VitalsReadings.from(submission("s3", "Vitals", data))!!
        assertEquals("140 mmHg (systolic)", vitals.bloodPressure)
    }

    @Test
    fun `latest picks the newest submitted vitals and ignores drafts and other forms`() {
        val older = submission(
            "old", "Vitals", buildJsonObject { put("heartRate", 60) },
            submittedAt = "2026-09-23T09:00:00Z",
        )
        val draft = submission(
            "draft", "Vitals", buildJsonObject { put("heartRate", 999) },
            status = "DRAFT", submittedAt = "2026-09-25T09:00:00Z",
        )
        val clinicalNote = submission(
            "note", "Clinical Note", buildJsonObject { put("assessment", "stable") },
            submittedAt = "2026-09-25T10:00:00Z",
        )
        val newest = submission(
            "new", "Vitals", buildJsonObject { put("heartRate", 75) },
            submittedAt = "2026-09-24T12:00:00Z",
        )

        val vitals = VitalsReadings.latest(listOf(clinicalNote, draft, older, newest))!!

        assertEquals(75.0, com.ehealthinformatics.prognocare.feature.forms.SchemaKeyMapper.primitive(
            buildJsonObject { put("heartRate", 75) },
            "heartRate",
        ))
        assertEquals("75 bpm", vitals.heartRate)
        assertEquals("2026-09-24T12:00:00Z", vitals.recordedAt)
    }

    @Test
    fun `no vitals submissions yields null`() {
        val clinicalNote = submission(
            "note", "Clinical Note", buildJsonObject { put("assessment", "stable") },
        )
        assertNull(VitalsReadings.latest(listOf(clinicalNote)))
        assertNull(VitalsReadings.from(submission("s4", "Vitals", buildJsonObject { })))
    }

    @Test
    fun `rows are ordered and only include present values`() {
        val vitals = VitalsReadings.from(submission("s5", "Vitals", seedSchemaData))!!
        val labels = vitals.rows().map { it.label }
        assertEquals(
            listOf(
                "Temperature", "Blood pressure", "Heart rate", "Respiratory rate",
                "SpO₂", "Weight", "Height", "Notes",
            ),
            labels,
        )
    }
}
