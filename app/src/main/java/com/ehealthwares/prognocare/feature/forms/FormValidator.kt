package com.ehealthwares.prognocare.feature.forms

/**
 * Lightweight client-side validation mirroring the backend/web schema validation.
 * Container types (section/tab/col/table) are skipped for empty checks; tab/col
 * recurse into children.
 */
object FormValidator {

    fun validate(schema: FormSchemaData, data: FormData): List<String> {
        val errors = mutableListOf<String>()
        for (field in schema.fields) validateField(field, data, errors)
        return errors
    }

    private fun validateField(field: FormField, data: FormData, errors: MutableList<String>) {
        when (field.type) {
            FormFieldType.SECTION, FormFieldType.TABLE -> return
            FormFieldType.TAB, FormFieldType.COL -> {
                for (child in field.fields) validateField(child, data, errors)
                return
            }
            else -> Unit
        }

        val fieldValue = data[field.key]
        if (field.required && (fieldValue == null || fieldValue == "" || fieldValue == false)) {
            errors += "${field.label} is required"
            return
        }
        if (fieldValue == null || fieldValue == "") return

        when (field.type) {
            FormFieldType.NUMBER ->
                if (fieldValue !is Number || fieldValue.toString().toDoubleOrNull()?.let { it.isNaN() } == true) {
                    errors += "${field.label} must be a number"
                }
            FormFieldType.DATE, FormFieldType.DATETIME ->
                if (fieldValue !is String || !isValidDate(fieldValue)) {
                    errors += "${field.label} must be a valid date"
                }
            FormFieldType.SELECT, FormFieldType.RADIO ->
                if (field.options.isNotEmpty() && !field.options.contains(fieldValue.toString())) {
                    errors += "${field.label} has an invalid option"
                }
            FormFieldType.CHECKBOX_GROUP ->
                if (fieldValue is List<*> && field.options.isNotEmpty() &&
                    fieldValue.any { !field.options.contains(it.toString()) }
                ) {
                    errors += "${field.label} must be a valid selection"
                }
            else -> Unit
        }
    }

    private fun isValidDate(value: String): Boolean {
        val normalized = value.trim()
        if (normalized.isEmpty()) return false
        val regex = Regex(
            "^\\d{4}-\\d{2}-\\d{2}(T\\d{2}:\\d{2}(:\\d{2})?)?$",
        )
        if (!regex.matches(normalized)) return false
        return try {
            // Basic sanity: year 1000-9999, month 1-12, day 1-31
            val ymd = if (normalized.contains('T')) normalized.substringBefore('T') else normalized
            val parts = ymd.split("-").map { it.toInt() }
            parts[1] in 1..12 && parts[2] in 1..31
        } catch (_: Exception) {
            false
        }
    }
}
