package com.ehealthwares.prognocare

import com.ehealthwares.prognocare.feature.forms.FormFieldType
import com.ehealthwares.prognocare.feature.forms.FormSchemaParser
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FormSchemaParserTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `parses a schema with mixed field types`() {
        val schema = json.parseToJsonElement(
            """
            {
              "fields": [
                { "key": "reason", "label": "Reason", "type": "text", "required": true },
                { "key": "notes", "label": "Notes", "type": "textarea", "rows": 4 },
                { "key": "temperature", "label": "Temp", "type": "number", "defaultValue": 36.5 },
                { "key": "onDate", "label": "On", "type": "date" },
                { "key": "severity", "label": "Severity", "type": "select", "options": ["MILD", "MODERATE"] },
                { "key": "consent", "label": "Consent", "type": "checkbox" },
                { "key": "tags", "label": "Tags", "type": "checkbox-group", "options": ["A", "B"] }
              ]
            }
            """.trimIndent(),
        )

        val parsed = FormSchemaParser.parse(schema)

        assertEquals(7, parsed.fields.size)
        assertEquals(FormFieldType.TEXT, parsed.fields[0].type)
        assertTrue(parsed.fields[0].required)
        assertEquals(FormFieldType.TEXTAREA, parsed.fields[1].type)
        assertEquals(4, parsed.fields[1].rows)
        assertEquals(FormFieldType.NUMBER, parsed.fields[2].type)
        assertEquals(36.5, parsed.fields[2].defaultValue)
        assertEquals(listOf("MILD", "MODERATE"), parsed.fields[4].options)
        assertEquals(FormFieldType.CHECKBOX_GROUP, parsed.fields[6].type)
    }

    @Test
    fun `parses container types with children`() {
        val schema = json.parseToJsonElement(
            """
            {
              "fields": [
                { "key": "tabs", "label": "Tabs", "type": "tab", "fields": [
                  { "key": "tab1", "label": "Tab 1", "type": "tab", "fields": [
                    { "key": "color", "label": "Color", "type": "select", "options": ["RED"] }
                  ]}
                ]},
                { "key": "grid", "label": "Grid", "type": "col", "fields": [
                  { "key": "a", "label": "A", "type": "text" },
                  { "key": "b", "label": "B", "type": "number" }
                ]},
                { "key": "hr", "label": "Section", "type": "section" }
              ]
            }
            """.trimIndent(),
        )

        val parsed = FormSchemaParser.parse(schema)

        assertEquals(3, parsed.fields.size)
        assertEquals(FormFieldType.TAB, parsed.fields[0].type)
        assertEquals(1, parsed.fields[0].fields.size)
        assertEquals(FormFieldType.COL, parsed.fields[1].type)
        assertEquals(2, parsed.fields[1].fields.size)
        assertEquals(FormFieldType.SECTION, parsed.fields[2].type)
    }

    @Test
    fun `parses table columns`() {
        val schema = json.parseToJsonElement(
            """
            {
              "fields": [
                { "key": "medications", "label": "Medications", "type": "table", "columns": [
                  { "key": "name", "label": "Name", "type": "text" },
                  { "key": "dose", "label": "Dose", "type": "number" }
                ]}
              ]
            }
            """.trimIndent(),
        )

        val parsed = FormSchemaParser.parse(schema)

        assertEquals(FormFieldType.TABLE, parsed.fields[0].type)
        assertEquals(2, parsed.fields[0].columns.size)
        assertEquals("dose", parsed.fields[0].columns[1].key)
        assertEquals(FormFieldType.NUMBER, parsed.fields[0].columns[1].type)
    }

    @Test
    fun `returns empty schema for null or missing fields`() {
        assertTrue(FormSchemaParser.parse(null).fields.isEmpty())
        assertTrue(FormSchemaParser.parse(json.parseToJsonElement("{}")).fields.isEmpty())
    }

    @Test
    fun `unknown field type defaults to text`() {
        val schema = json.parseToJsonElement(
            """{ "fields": [ { "key": "x", "label": "X", "type": "weird" } ] }""",
        )
        assertEquals(FormFieldType.TEXT, FormSchemaParser.parse(schema).fields[0].type)
    }

    @Test
    fun `missing optional fields produce empty defaults`() {
        val schema = json.parseToJsonElement(
            """{ "fields": [ { "key": "onlyKey" } ] }""",
        )
        val field = FormSchemaParser.parse(schema).fields[0]
        assertNull(field.label.ifEmpty { null })
        assertEquals(FormFieldType.TEXT, field.type)
        assertEquals(emptyList<String>(), field.options)
    }

    @Test
    fun `parses annotate config`() {
        val schema = json.parseToJsonElement(
            """
            {
              "fields": [
                { "key": "regions", "label": "Regions", "type": "annotate",
                  "annotate": { "target": "body", "multiple": true, "regions": ["head", "neck"] } },
                { "key": "organ", "label": "Heart", "type": "annotate",
                  "annotate": { "target": "organ", "organ": "heart", "structures": ["left-ventricle"] } },
                { "key": "level", "label": "Level", "type": "annotate",
                  "annotate": { "target": "dermatome", "multiple": false } }
              ]
            }
            """.trimIndent(),
        )

        val parsed = FormSchemaParser.parse(schema)

        assertEquals(FormFieldType.ANNOTATE, parsed.fields[0].type)
        assertEquals(com.ehealthwares.prognocare.feature.forms.AnnotateTarget.BODY, parsed.fields[0].annotate?.target)
        assertEquals(listOf("head", "neck"), parsed.fields[0].annotate?.regions)

        assertEquals(com.ehealthwares.prognocare.feature.forms.AnnotateTarget.ORGAN, parsed.fields[1].annotate?.target)
        assertEquals("heart", parsed.fields[1].annotate?.organ)
        assertEquals(listOf("left-ventricle"), parsed.fields[1].annotate?.structures)

        assertEquals(com.ehealthwares.prognocare.feature.forms.AnnotateTarget.DERMATOME, parsed.fields[2].annotate?.target)
        assertEquals(false, parsed.fields[2].annotate?.multiple)
    }

    @Test
    fun `parses file and lookup field types`() {
        val schema = json.parseToJsonElement(
            """
            {
              "fields": [
                { "key": "doc", "label": "Doc", "type": "file", "accept": "application/pdf", "multiple": true, "maxSizeMb": 5 },
                { "key": "who", "label": "Who", "type": "patient" },
                { "key": "by", "label": "By", "type": "practitioner" },
                { "key": "dept", "label": "Dept", "type": "department" },
                { "key": "code", "label": "Code", "type": "loinc-test" }
              ]
            }
            """.trimIndent(),
        )

        val parsed = FormSchemaParser.parse(schema)

        assertEquals(FormFieldType.FILE, parsed.fields[0].type)
        assertEquals("application/pdf", parsed.fields[0].accept)
        assertTrue(parsed.fields[0].multiple)
        assertEquals(5, parsed.fields[0].maxSizeMb)

        assertEquals(FormFieldType.PATIENT, parsed.fields[1].type)
        assertEquals(FormFieldType.PRACTITIONER, parsed.fields[2].type)
        assertEquals(FormFieldType.DEPARTMENT, parsed.fields[3].type)
        assertEquals(FormFieldType.LOINC_TEST, parsed.fields[4].type)
    }
}