package com.ehealthwares.prognocare

import com.ehealthwares.prognocare.feature.forms.FormData
import com.ehealthwares.prognocare.feature.forms.FormField
import com.ehealthwares.prognocare.feature.forms.FormFieldType
import com.ehealthwares.prognocare.feature.forms.FormSchemaData
import com.ehealthwares.prognocare.feature.forms.FormValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FormValidatorTest {

    private fun field(
        key: String,
        label: String,
        type: FormFieldType,
        required: Boolean = false,
        options: List<String> = emptyList(),
    ) = FormField(key = key, label = label, type = type, required = required, options = options)

    @Test
    fun `required text field is enforced`() {
        val schema = FormSchemaData(listOf(field("reason", "Reason", FormFieldType.TEXT, required = true)))
        val errors = FormValidator.validate(schema, emptyMap())
        assertEquals(listOf("Reason is required"), errors)
    }

    @Test
    fun `no errors when required field is filled`() {
        val schema = FormSchemaData(listOf(field("reason", "Reason", FormFieldType.TEXT, required = true)))
        val errors = FormValidator.validate(schema, mapOf("reason" to "Headache"))
        assertTrue(errors.isEmpty())
    }

    @Test
    fun `number field rejects non-numeric`() {
        val schema = FormSchemaData(listOf(field("temp", "Temp", FormFieldType.NUMBER)))
        val errors = FormValidator.validate(schema, mapOf("temp" to "abc"))
        assertTrue(errors.any { it.contains("must be a number") })
    }

    @Test
    fun `date field rejects malformed value`() {
        val schema = FormSchemaData(listOf(field("on", "On", FormFieldType.DATE)))
        val errors = FormValidator.validate(schema, mapOf("on" to "not-a-date"))
        assertTrue(errors.any { it.contains("must be a valid date") })
    }

    @Test
    fun `valid iso date passes`() {
        val schema = FormSchemaData(listOf(field("on", "On", FormFieldType.DATE)))
        val errors = FormValidator.validate(schema, mapOf("on" to "2026-01-05"))
        assertTrue(errors.isEmpty())
    }

    @Test
    fun `recurse into tab and col containers`() {
        val child = field("a", "A", FormFieldType.TEXT, required = true)
        val tab = FormField(
            key = "t",
            label = "Tabs",
            type = FormFieldType.TAB,
            fields = listOf(FormField(key = "x", label = "X", type = FormFieldType.COL, fields = listOf(child))),
        )
        val errors = FormValidator.validate(FormSchemaData(listOf(tab)), emptyMap())
        assertEquals(listOf("A is required"), errors)
    }

    @Test
    fun `checkbox-group validates selections against options`() {
        val schema = FormSchemaData(listOf(field("tags", "Tags", FormFieldType.CHECKBOX_GROUP, options = listOf("A", "B"))))
        val valid = FormValidator.validate(schema, mapOf("tags" to listOf("A")))
        assertTrue(valid.isEmpty())
    }

    @Test
    fun `missing required checkbox is flagged`() {
        val schema = FormSchemaData(listOf(field("consent", "Consent", FormFieldType.CHECKBOX, required = true)))
        val errors = FormValidator.validate(schema, mapOf("consent" to false))
        assertTrue(errors.any { it.contains("Consent is required") })
    }
}