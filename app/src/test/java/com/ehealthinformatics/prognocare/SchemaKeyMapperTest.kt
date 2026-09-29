package com.ehealthinformatics.prognocare

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests the concept → schema-key mapping used by form submissions (vitals). */
class SchemaKeyMapperTest {

    private fun schema(vararg fields: Pair<String, String>) = buildJsonObject {
        putJsonArray("fields") {
            fields.forEach { (key, type) ->
                add(buildJsonObject {
                    put("key", key)
                    put("label", key)
                    put("type", type)
                })
            }
        }
    }

    private val raw = mapOf(
        "temperature" to 37.5,
        "heartRate" to 72L,
        "bpSystolic" to 120L,
        "bpDiastolic" to 80L,
        "respiratoryRate" to 16L,
        "oxygenSaturation" to 98L,
        "weight" to 70.5,
        "height" to 175.0,
        "notes" to "Patient stable",
    )

    @Test
    fun `maps concepts onto the seeded camelCase vitals schema`() {
        val schema = schema(
            "temperature" to "number",
            "heartRate" to "number",
            "respiratoryRate" to "number",
            "bloodPressureSystolic" to "number",
            "bloodPressureDiastolic" to "number",
            "oxygenSaturation" to "number",
            "weight" to "number",
            "height" to "number",
            "notes" to "textarea",
        )

        val payload = com.ehealthinformatics.prognocare.feature.forms.SchemaKeyMapper.map(schema, raw)

        assertEquals(9, payload.size)
        assertEquals(37.5, com.ehealthinformatics.prognocare.feature.forms.SchemaKeyMapper.primitive(payload, "temperature"))
        assertEquals(72.0, com.ehealthinformatics.prognocare.feature.forms.SchemaKeyMapper.primitive(payload, "heartRate"))
        assertEquals(120.0, com.ehealthinformatics.prognocare.feature.forms.SchemaKeyMapper.primitive(payload, "bloodPressureSystolic"))
        assertEquals(80.0, com.ehealthinformatics.prognocare.feature.forms.SchemaKeyMapper.primitive(payload, "bloodPressureDiastolic"))
        assertEquals(98.0, com.ehealthinformatics.prognocare.feature.forms.SchemaKeyMapper.primitive(payload, "oxygenSaturation"))
        assertEquals("Patient stable", com.ehealthinformatics.prognocare.feature.forms.SchemaKeyMapper.primitive(payload, "notes"))
    }

    @Test
    fun `maps concepts onto snake_case schemas`() {
        val schema = schema(
            "temperature" to "number",
            "heart_rate" to "number",
            "bp_systolic" to "number",
            "bp_diastolic" to "number",
            "oxygen_saturation" to "number",
        )

        val payload = com.ehealthinformatics.prognocare.feature.forms.SchemaKeyMapper.map(schema, raw)

        assertEquals(5, payload.size)
        assertEquals(72.0, com.ehealthinformatics.prognocare.feature.forms.SchemaKeyMapper.primitive(payload, "heart_rate"))
        assertEquals(120.0, com.ehealthinformatics.prognocare.feature.forms.SchemaKeyMapper.primitive(payload, "bp_systolic"))
        assertEquals(98.0, com.ehealthinformatics.prognocare.feature.forms.SchemaKeyMapper.primitive(payload, "oxygen_saturation"))
    }

    @Test
    fun `matches suffixed schema keys by containment`() {
        val schema = schema(
            "bloodPressureSystolicMmHg" to "number",
            "temperatureCelsius" to "number",
        )
        val payload = com.ehealthinformatics.prognocare.feature.forms.SchemaKeyMapper.map(schema, raw)
        assertEquals(120.0, com.ehealthinformatics.prognocare.feature.forms.SchemaKeyMapper.primitive(payload, "bloodPressureSystolicMmHg"))
        assertEquals(37.5, com.ehealthinformatics.prognocare.feature.forms.SchemaKeyMapper.primitive(payload, "temperatureCelsius"))
    }

    @Test
    fun `drops values without a schema field and handles nesting`() {
        val schema = buildJsonObject {
            putJsonArray("fields") {
                add(buildJsonObject {
                    put("key", "vitalsSection")
                    put("label", "Vitals")
                    put("type", "section")
                    putJsonArray("fields") {
                        add(buildJsonObject {
                            put("key", "heartRate")
                            put("label", "Heart Rate")
                            put("type", "number")
                        })
                    }
                })
                add(buildJsonObject {
                    put("key", "mood")
                    put("label", "Mood")
                    put("type", "text")
                })
            }
        }

        val payload = com.ehealthinformatics.prognocare.feature.forms.SchemaKeyMapper.map(schema, raw)

        // heartRate found inside the section; everything else dropped.
        assertEquals(1, payload.size)
        assertEquals(72.0, com.ehealthinformatics.prognocare.feature.forms.SchemaKeyMapper.primitive(payload, "heartRate"))
        assertFalse(payload.containsKey("mood"))
        assertFalse(payload.containsKey("temperature"))
    }

    @Test
    fun `skips blank values`() {
        val schema = schema("temperature" to "number", "notes" to "textarea")
        val payload = com.ehealthinformatics.prognocare.feature.forms.SchemaKeyMapper.map(
            schema,
            mapOf("temperature" to "", "notes" to "   "),
        )
        assertTrue(payload.isEmpty())
    }

    @Test
    fun `missing or malformed schema passes the bag through unchanged`() {
        val mapper = com.ehealthinformatics.prognocare.feature.forms.SchemaKeyMapper

        val nullSchemaPayload = mapper.map(null, raw)
        assertEquals(raw.keys, nullSchemaPayload.keys)

        // No parseable fields array — cannot map, so pass values through.
        val noFieldsPayload = mapper.map(buildJsonObject { put("unexpected", 1) }, raw)
        assertEquals(raw.size, noFieldsPayload.size)
        assertTrue(noFieldsPayload.containsKey("temperature"))
    }

    @Test
    fun `remap resolves fields from concept keys and drops unknown entries`() {
        val mapper = com.ehealthinformatics.prognocare.feature.forms.SchemaKeyMapper
        val schema = schema(
            "bloodPressureSystolic" to "number",
            "heartRate" to "number",
            "chiefComplaint" to "textarea",
        )
        val bag = mapOf(
            "bpSystolic" to kotlinx.serialization.json.JsonPrimitive(120),
            "pulse" to kotlinx.serialization.json.JsonPrimitive(72),
            "chiefComplaint" to kotlinx.serialization.json.JsonPrimitive("Chest pain"),
            "junkKey" to kotlinx.serialization.json.JsonPrimitive("noise"),
        )
        val payload = mapper.remap(schema, bag)

        assertEquals(3, payload.size)
        assertEquals(120.0, mapper.primitive(payload, "bloodPressureSystolic"))
        assertEquals(72.0, mapper.primitive(payload, "heartRate"))
        assertEquals("Chest pain", mapper.primitive(payload, "chiefComplaint"))
    }

    @Test
    fun `remap prefers exact keys and consumes each entry once`() {
        val mapper = com.ehealthinformatics.prognocare.feature.forms.SchemaKeyMapper
        val schema = schema(
            "pulse" to "number",
            "heartRate" to "number",
        )
        val bag = mapOf(
            "heartRate" to kotlinx.serialization.json.JsonPrimitive(72),
        )
        val payload = mapper.remap(schema, bag)

        // Exact match wins for heartRate; pulse then falls back to the same
        // bag entry's alias — but an entry is consumed once, so pulse (declared
        // first in the schema walk) claims it and heartRate is left empty.
        assertEquals(1, payload.size)
    }

    @Test
    fun `remap drops null entries`() {
        val mapper = com.ehealthinformatics.prognocare.feature.forms.SchemaKeyMapper
        val schema = schema("temperature" to "number", "heartRate" to "number")
        val payload = mapper.remap(
            schema,
            mapOf(
                "temperature" to null,
                "heartRate" to kotlinx.serialization.json.JsonNull,
                "pulse" to kotlinx.serialization.json.JsonPrimitive(80),
            ),
        )
        assertEquals(1, payload.size)
        assertEquals(80.0, mapper.primitive(payload, "heartRate"))
    }
}
